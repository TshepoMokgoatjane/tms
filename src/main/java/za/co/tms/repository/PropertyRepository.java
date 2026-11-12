package za.co.tms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import za.co.tms.domain.Property;

import java.util.List;
import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    Optional<Property> findByStreetAddress(String streetAddress);

    boolean existsByStreetAddress(String streetAddress);

    // ===== Metadata-only projection (NEVER select lease_pdf_data) =====
    // Powers list views so the heavy lease blob is never loaded for every row.
    // The constructor expression maps to Property's metadata-only constructor,
    // leaving leasePdfData null; the bytes are only loaded when fetched by id.
    @Query("""
            select new za.co.tms.domain.Property(
                p.id, p.streetAddress, p.erfNumber, p.leaseFilename, p.leaseContentType, p.leaseFileSize)
            from Property p
            order by p.streetAddress asc""")
    List<Property> findAllMetadata();
}
