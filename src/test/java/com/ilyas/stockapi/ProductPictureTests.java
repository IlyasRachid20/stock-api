package com.ilyas.stockapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Product pictures: every upload is saved again as a JPEG of at most 1200 pixels, without the
 * file's hidden data, and served to everyone. (A cashier can't upload: see RoleAccessTests.)
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
@Transactional
class ProductPictureTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void pictureIsShrunkSavedAsJpegAndServedToEveryone() throws Exception {
		long product = createProduct("Galaxy S26");

		String url = JsonPath.read(upload(product, png(2400, 1600))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.width").value(1200))
				.andExpect(jsonPath("$.height").value(800))
				.andReturn().getResponse().getContentAsString(), "$.url");

		mockMvc.perform(get("/api/products/" + product))
				.andExpect(jsonPath("$.images[*].url", contains(url)));

		// No login needed: an <img> can't send the token
		byte[] jpeg = mockMvc.perform(get(url).with(anonymous()))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.IMAGE_JPEG))
				.andExpect(header().string("Cache-Control", containsString("max-age=31536000")))
				.andReturn().getResponse().getContentAsByteArray();
		BufferedImage served = ImageIO.read(new ByteArrayInputStream(jpeg));
		assertThat(served.getWidth()).isEqualTo(1200);
		assertThat(served.getHeight()).isEqualTo(800);
	}

	@Test
	void hiddenDataInTheFileIsNotKept() throws Exception {
		long product = createProduct("Galaxy S26");
		byte[] withComment = withJpegComment(jpeg(300, 300), "GPS 33.5731 -7.5898 taken at home");

		String url = JsonPath.read(upload(product, withComment).andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString(), "$.url");

		byte[] stored = mockMvc.perform(get(url)).andReturn().getResponse().getContentAsByteArray();
		assertThat(new String(stored, StandardCharsets.ISO_8859_1)).doesNotContain("GPS");
	}

	@Test
	void fileThatIsNotAPictureIsRejected() throws Exception {
		long product = createProduct("Galaxy S26");

		upload(product, "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("The file is not a picture the server can read: use a JPEG or PNG"));
	}

	@Test
	void hugePictureIsRejectedBeforeBeingDecoded() throws Exception {
		long product = createProduct("Galaxy S26");

		// A tiny file whose header claims 20000 x 20000 pixels (1.6 GB once decoded)
		upload(product, pngHeaderOnly(20_000, 20_000))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("The picture is too large (20000 x 20000 pixels): resize it first"));
	}

	@Test
	void anyPictureCanBecomeTheCover() throws Exception {
		long product = createProduct("Galaxy S26");
		long first = idOf(upload(product, png(200, 200)));
		long second = idOf(upload(product, png(200, 200)));

		mockMvc.perform(put("/api/products/" + product + "/images/" + second + "/cover"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].id", contains((int) second, (int) first)));
		mockMvc.perform(get("/api/products/" + product))
				.andExpect(jsonPath("$.images[*].id", contains((int) second, (int) first)));
	}

	@Test
	void aProductHasSixPicturesAtMost() throws Exception {
		long product = createProduct("Galaxy S26");
		for (int i = 0; i < 6; i++) {
			upload(product, png(50, 50)).andExpect(status().isCreated());
		}

		upload(product, png(50, 50))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("A product can have 6 pictures at most: delete one first"));
	}

	@Test
	void deletedPictureIsGone() throws Exception {
		long product = createProduct("Galaxy S26");
		long picture = idOf(upload(product, png(100, 100)));

		mockMvc.perform(delete("/api/products/" + product + "/images/" + picture)).andExpect(status().isNoContent());

		mockMvc.perform(get("/api/images/" + picture)).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/products/" + product)).andExpect(jsonPath("$.images.length()").value(0));
	}

	@Test
	void pictureCanOnlyBeChangedThroughItsOwnProduct() throws Exception {
		long product = createProduct("Galaxy S26");
		long other = createProduct("iPhone 17");
		long picture = idOf(upload(product, png(100, 100)));

		mockMvc.perform(delete("/api/products/" + other + "/images/" + picture)).andExpect(status().isNotFound());
		mockMvc.perform(put("/api/products/" + other + "/images/" + picture + "/cover")).andExpect(status().isNotFound());
	}

	@Test
	void productThatWasNeverSoldIsDeletedWithItsPictures() throws Exception {
		long product = createProduct("Galaxy S26");
		long picture = idOf(upload(product, png(100, 100)));

		mockMvc.perform(delete("/api/products/" + product)).andExpect(status().isNoContent());

		mockMvc.perform(get("/api/images/" + picture)).andExpect(status().isNotFound());
	}

	@Test
	void uploadWithoutAFileIsExplained() throws Exception {
		long product = createProduct("Galaxy S26");

		mockMvc.perform(multipart("/api/products/" + product + "/images"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Send the file as the 'file' part of a multipart/form-data request"));
	}

	@Test
	void descriptionIsOptionalTrimmedAndLimited() throws Exception {
		mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Charger\",\"price\":199.00,\"description\":\"  Fast charger  \"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.description").value("Fast charger"));

		mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Charger\",\"price\":199.00,\"description\":\"" + "x".repeat(2001) + "\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.description").exists());
	}

	private ResultActions upload(long product, byte[] bytes) throws Exception {
		return mockMvc.perform(multipart("/api/products/" + product + "/images")
				.file(new MockMultipartFile("file", "photo", "application/octet-stream", bytes)));
	}

	private long createProduct(String name) throws Exception {
		return idOf(mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"" + name + "\",\"price\":100.00}")).andExpect(status().isCreated()));
	}

	private static long idOf(ResultActions result) throws Exception {
		Number id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
		return id.longValue();
	}

	private static BufferedImage picture(int width, int height) {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setPaint(new GradientPaint(0, 0, Color.BLUE, width, height, Color.ORANGE));
		graphics.fillRect(0, 0, width, height);
		graphics.dispose();
		return image;
	}

	private static byte[] png(int width, int height) throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ImageIO.write(picture(width, height), "png", bytes);
		return bytes.toByteArray();
	}

	private static byte[] jpeg(int width, int height) throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ImageIO.write(picture(width, height), "jpeg", bytes);
		return bytes.toByteArray();
	}

	// Inserts a comment segment (FF FE) right after the JPEG's start marker, like a camera's extra data
	private static byte[] withJpegComment(byte[] jpeg, String comment) {
		byte[] text = comment.getBytes(StandardCharsets.ISO_8859_1);
		ByteBuffer out = ByteBuffer.allocate(jpeg.length + 4 + text.length);
		out.put(jpeg, 0, 2);
		out.put((byte) 0xFF).put((byte) 0xFE).putShort((short) (text.length + 2)).put(text);
		out.put(jpeg, 2, jpeg.length - 2);
		return out.array();
	}

	// A PNG signature and IHDR chunk only: enough for a reader to learn the (claimed) size
	private static byte[] pngHeaderOnly(int width, int height) {
		ByteBuffer ihdr = ByteBuffer.allocate(17);
		ihdr.put("IHDR".getBytes(StandardCharsets.US_ASCII)).putInt(width).putInt(height)
				.put((byte) 8).put((byte) 2).put((byte) 0).put((byte) 0).put((byte) 0);
		CRC32 crc = new CRC32();
		crc.update(ihdr.array());
		ByteBuffer png = ByteBuffer.allocate(8 + 4 + 17 + 4);
		png.put(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
		png.putInt(13).put(ihdr.array()).putInt((int) crc.getValue());
		return png.array();
	}

}
