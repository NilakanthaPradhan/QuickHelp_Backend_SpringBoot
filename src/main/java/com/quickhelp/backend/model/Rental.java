package com.quickhelp.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Data
public class Rental {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String title;
    @Column(length = 2000)
    private String description;
    private String price; // e.g. "₹8000/mo"
    private String location;
    private Double lat;
    private Double lng;
    
    private String tenantType; // FAMILY, BACHELOR
    private String ownerName;
    private String ownerPhone;
    
    @ElementCollection
    private List<String> amenities;

    @ElementCollection
    @Column(columnDefinition = "TEXT")
    private List<String> photos; // Base64 strings
}
