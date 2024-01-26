package com.zyniel.apps.westiemosaic.controllers;

import com.zyniel.apps.westiemosaic.entities.WestieEvent;
import com.zyniel.apps.westiemosaic.models.helpers.ConfigurationHelper;
import com.zyniel.apps.westiemosaic.services.impls.EventService;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@RestController
public class EventController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventController.class);

    @Autowired
    EventService eventService = null;

    @GetMapping("/events")
    public List<WestieEvent> findAll() {
        return eventService.findAll();
    }

    @GetMapping(value = "/events/{id}")
    public WestieEvent find(@PathVariable("id") long id) {
        Optional<WestieEvent>  optEvent = eventService.findById(id);
        WestieEvent event = null;
        if (optEvent.isPresent()) {
            event = optEvent.get();
        }
        return event;
    }

    @GetMapping(value = "/image")
    @Cacheable("images")
    public @ResponseBody byte[] getImage(@RequestParam("path") String bannerUrl) throws IOException {
        InputStream in =  new FileInputStream(Path.of(ConfigurationHelper.getLocalImageRepo(),bannerUrl).toString());
        return IOUtils.toByteArray(in);
    }

}