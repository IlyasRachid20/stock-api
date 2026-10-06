package com.ilyas.stockapi.entity;

import jakarta.persistence.*;

/** The JPEG bytes of a product picture, kept apart so that loading pictures' details stays light. */
@Entity
@Table(name = "product_image_files")
public class ProductImageFile {

    // The ProductImage's id
    @Id
    @Column(name = "image_id")
    private Long imageId;

    @Column(nullable = false)
    private byte[] content;

    protected ProductImageFile() {
    }

    public ProductImageFile(Long imageId, byte[] content) {
        this.imageId = imageId;
        this.content = content;
    }

    public Long getImageId() { return imageId; }

    public byte[] getContent() { return content; }
}
