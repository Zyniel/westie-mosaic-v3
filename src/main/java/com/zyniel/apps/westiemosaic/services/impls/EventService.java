package com.zyniel.apps.westiemosaic.services.impls;

import com.zyniel.apps.westiemosaic.repositories.EventRepository;
import com.zyniel.apps.westiemosaic.entities.WestieEvent;
import com.zyniel.apps.westiemosaic.services.IEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EventService implements IEventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventService.class);

    @Autowired
    EventRepository eventRepository;

    @Override
    public List<WestieEvent> findAll() {
        return eventRepository.findAll();
    }

    @Override
    public Optional<WestieEvent> findById(long id) {
        return eventRepository.findById(id);
    }

    @Override
    public List<WestieEvent> findAllByCity(String city) {
        return eventRepository.findByCity(city);
    }

    @Override
    public List<WestieEvent> findAllByCountry(String country) {
        return eventRepository.findByCountry(country);
    }

    public WestieEvent saveEvent(WestieEvent event) {
        return eventRepository.save(event);
    }
}