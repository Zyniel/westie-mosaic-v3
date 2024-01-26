package com.zyniel.apps.westiemosaic.services;

import com.zyniel.apps.westiemosaic.entities.WestieEvent;

import java.util.List;
import java.util.Optional;

public interface IEventService {

    List<WestieEvent> findAll();

    Optional<WestieEvent> findById(long id);

    List<WestieEvent> findAllByCity(String city);

    List<WestieEvent> findAllByCountry(String country);

    WestieEvent saveEvent(WestieEvent event);

}