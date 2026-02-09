package com.quickhelp.backend.controller;

import com.quickhelp.backend.model.Rental;
import com.quickhelp.backend.repository.RentalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rentals")
@CrossOrigin(origins = "*")
public class RentalController {

    @Autowired
    private RentalRepository rentalRepository;

    @GetMapping
    public List<Rental> getAllRentals() {
        return rentalRepository.findAll();
    }

    @PostMapping
    public Rental createRental(@RequestBody Rental rental) {
        return rentalRepository.save(rental);
    }
    
    @GetMapping("/{id}")
    public Rental getRental(@PathVariable Long id) {
        return rentalRepository.findById(id).orElse(null);
    }
    
    @DeleteMapping("/{id}")
    public void deleteRental(@PathVariable Long id) {
        rentalRepository.deleteById(id);
    }
}
