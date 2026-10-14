package za.co.tms.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A photo attached to a maintenance support ticket. Stored as bytes in the database,
 * mirroring {@link GalleryImage}. Linked to a ticket by {@code ticketId}. Capped per
 * ticket at the service layer; purged together with the ticket by the retention job.
 */
@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Photo attached to a maintenance support ticket")
public class TicketImage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    @Schema(description = "Owning ticket id", example = "12")
    @Column(nullable = false)
    private Long ticketId;

    @Schema(description = "Original uploaded filename", example = "leak.jpg")
    private String originalFilename;

    @Schema(description = "MIME type", example = "image/jpeg")
    private String contentType;

    @Schema(description = "File size in bytes", example = "245000")
    private Long fileSize;

    @Lob
    @JsonIgnore
    @Column(name = "image_data", columnDefinition = "LONGBLOB")
    @Schema(hidden = true)
    private byte[] imageData;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    public void prePersist() {
        if (this.uploadedAt == null) {
            this.uploadedAt = LocalDateTime.now();
        }
    }
}
