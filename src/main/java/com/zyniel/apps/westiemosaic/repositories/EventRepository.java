package com.zyniel.apps.westiemosaic.repositories;

import com.zyniel.apps.westiemosaic.entities.WestieEvent;
import org.springframework.data.repository.ListCrudRepository;

import java.util.Date;
import java.util.List;

public interface EventRepository extends ListCrudRepository<WestieEvent, Long> {

    List<WestieEvent> findByStartDate(Date startDate);

    List<WestieEvent> findByEndDate(Date startDate);

    List<WestieEvent> findByName(String country);

    List<WestieEvent> findByCountry(String country);

    List<WestieEvent> findByCity(String city);

}