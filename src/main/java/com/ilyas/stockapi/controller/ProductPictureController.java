package com.ilyas.stockapi.controller;

import com.ilyas.stockapi.dto.ProductImageResponse;
import com.ilyas.stockapi.service.ProductPictureService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
public class ProductPictureController {

    private final ProductPictureService pictureService;

    public ProductPictureController(ProductPictureService pictureService) {
        this.pictureService = pictureService;
    }

    // Upload as multipart/form-data with the picture in the "file" part (ADMIN only)
    @PostMapping(value = "/api/products/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ProductImageResponse add(@PathVariable Long id, @RequestPart("file") MultipartFile file) throws IOException {
        return pictureService.add(id, file.getInputStream(), file.getSize());
    }

    // Moves the picture first: it becomes the product's cover (ADMIN only)
    @PutMapping("/api/products/{id}/images/{imageId}/cover")
    public List<ProductImageResponse> makeCover(@PathVariable Long id, @PathVariable Long imageId) {
        return pictureService.makeCover(id, imageId);
    }

    @DeleteMapping("/api/products/{id}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @PathVariable Long imageId) {
        pictureService.delete(id, imageId);
    }

    // The JPEG, open to everyone (an <img> can't send the login token). A picture never changes:
    // a new upload gets a new id, so browsers may keep it for a year.
    @GetMapping("/api/images/{imageId}")
    public ResponseEntity<byte[]> image(@PathVariable Long imageId) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
                .body(pictureService.content(imageId));
    }
}
