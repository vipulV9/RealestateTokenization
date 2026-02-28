package com.product.RealestateTokenization.repository;


import com.product.RealestateTokenization.model.Property;
import com.product.RealestateTokenization.model.PropertyState;
import com.product.RealestateTokenization.model.PropertyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface PropertyRepo extends JpaRepository<Property, Long> {
    List<Property> findByStatusIn(List<String> statuses);

    List<Property> findByType(PropertyType type);
    List<Property> findByStateAndIsliveTrueAndIsApprovedTrue(PropertyState state);
    List<Property> findByTypeIn(List<PropertyType> types);
    List<Property> findByIsApproved(Boolean isApproved);
    List<Property> findByOwnerId(Long ownerId);
    Optional<Property> findByIdAndOwnerId(Long id, Long ownerId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Property p WHERE p.id = :id")
    Optional<Property> findByIdForUpdate(Long id);

    @Query(value = """
SELECT * FROM property p
WHERE p.state = 'NORMAL'
AND p.islive = true
AND p.is_approved = true
AND (
    6371 * acos(
        cos(radians(:lat)) *
        cos(radians(p.lat)) *
        cos(radians(p.lng) - radians(:lng)) +
        sin(radians(:lat)) *
        sin(radians(p.lat))
    )
) <= 6
""", nativeQuery = true)
    List<Property> findNormalPropertiesWithin6Km(double lat, double lng);

}
