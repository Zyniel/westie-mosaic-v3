package com.zyniel.apps.westiemosaic.controllers;

import com.zyniel.apps.westiemosaic.services.impls.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private static final Logger LOGGER = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    EventService eventService = null;

    @GetMapping("/")
    public String viewHomePage(Model model) {
        model.addAttribute("allevents", eventService.findAll());
        return "/index";
    }
}
