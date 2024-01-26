package com.zyniel.apps.westiemosaic.entities;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.apache.logging.log4j.util.Strings;
import org.hibernate.type.YesNoConverter;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

@Entity
@Table(name = "events")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
public abstract class WestieEvent {

    /***
     * Hibernate no-arguments default constructor
     */
    WestieEvent() {}

    @Id
    @Column(name = "id")
    protected String id = "";

    @Column(name = "name")
    @NotNull
    @NotBlank
    protected String name = "";

    @Column(name = "start_date")
    @Temporal(TemporalType.DATE)
    @NotNull
    protected Date startDate;

    @Column(name = "end_date")
    @Temporal(TemporalType.DATE)
    @NotNull
    protected Date endDate;

    @Column(name = "city")
    @NotNull
    @NotBlank
    protected String city = "";

    @Column(name = "country")
    @NotNull
    @NotBlank
    protected String country = "";

    @Column(name = "location")
    protected String fullLocation = "";

    @Column(name = "facebook_url")
    protected String facebookUrl = "";

    @Column(name = "website_url")
    protected String websiteUrl = "";

    @Column(name = "banner_url")
    protected String bannerUrl = "";

    @Column(name = "image_file")
    protected String imageFile = "";

    @Column(name = "is_wsdc")
    @Convert(converter = YesNoConverter.class)
    protected boolean isWSDC = false;

    /***
     * Westie Event with minimal mandatory information
     * @param name Name of the event
     * @param startDate Day on which the event starts. Must be prior or equal to the end date.
     * @param endDate Day on which the event ends. Must be later or equal to the start date.
     * @param city Name of the city hosting the event
     * @param country Name of the country hosting the event
     */
    public WestieEvent(String name, Date startDate, Date endDate, String city, String country) {
        setName(name);
        setStartDate(startDate);
        setEndDate(endDate);
        setCity(city);
        setCountry(country);
        setCompetitive(false);
        setId();
    }

    /**
     * @return the id as a concatenation of the event name, start and end dates.
     */
    public String getId() {
        return this.id;
    }

    /**
     * Creates a unique id based con the concatenation of the event name, start and end dates.
     */
    private void setId() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        this.id = this.name
                .replace(' ', '_')
                .concat("-")
                .concat(formatter.format(this.startDate))
                .concat("-")
                .concat(formatter.format(this.endDate))
                .toUpperCase();
    }

    /***
     *
     * @return flag to tag WSDC competitive events.
     */
    public boolean isCompetitive() {
        return isWSDC;
    }

    /**
     * @param WSDC flags the events as a competitive WSDC event
     */
    public void setCompetitive(boolean WSDC) {
        isWSDC = WSDC;
    }

    /***
     *
     * @return event name
     */
    public String getName() {
        return name;
    }

    /***
     *
     * @param name event name (must not be empty or null)
     */
    public void setName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Event 'Name' must not be Null");
        } else if (name.isEmpty()) {
            throw new IllegalArgumentException("Event 'Name' must not be empty");
        }
        this.name = name;
    }

    /***
     *
     * @return day on which the event starts
     */
    public Date getStartDate() {
        return startDate;
    }

    /***
     *
     * @param startDate day on which the event starts (must be non-null and prior or equal to the end date)
     */
    public void setStartDate(Date startDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("Event 'Start Date' cannot be null");
        } else if ((this.endDate != null) && (startDate.compareTo(this.endDate) > 0)) {
            throw new IllegalArgumentException("Event 'Start Date' must be lower or equal to 'End Date'");
        }
        this.startDate = startDate;
    }

    /**
     * @return day on which the event ends
     */
    public Date getEndDate() {
        return endDate;
    }

    /***
     *
     * @param endDate day on which the event ends (must be non-null and later or equal to the start date)
     */
    public void setEndDate(Date endDate) {
        if (endDate == null) {
            throw new IllegalArgumentException("Event 'End Date' cannot be null");
        } else if ((this.startDate != null) && (endDate.compareTo(this.startDate) < 0)) {
            throw new IllegalArgumentException("Event 'End Date' must be greater or equal to 'Start Date'");
        }
        this.endDate = endDate;
    }

    /***
     *
     * @return city name hosting the event
     */
    public String getCity() {
        return city;
    }

    /***
     *
     * @param city Optional city name hosting the event (optional)
     */
    public void setCity(String city) {
        this.city = city;
    }

    /***
     *
     * @return country name hosting the event
     */
    public String getCountry() {
        return country;
    }

    /***
     *
     * @param country Optional country name hosting the event (optional)
     */
    public void setCountry(String country) {
        this.country = country;
    }

    /***
     *
     * @return Optional full venue name, address, city and country to locate the event
     */
    public String getFullLocation() {
        return fullLocation;
    }

    /***
     *
     * @param fullLocation Optional full venue name, address, city and country to locate the event
     */
    public void setFullLocation(String fullLocation) {
        this.fullLocation = fullLocation;
    }

    /***
     *
     * @return facebook URL as string to find further information regarding the event
     */
    public String getFacebookUrl() {
        return facebookUrl;
    }

    /***
     *
     * @param facebookUrl Optional facebook URL as string to find further information regarding the event
     */
    public void setFacebookUrl(String facebookUrl) throws URISyntaxException, MalformedURLException {
        if (Strings.isNotBlank(facebookUrl)) {
            URI i = new URI(facebookUrl);
            URL u = i.toURL();
            this.facebookUrl = u.toString();
        } else {
            this.facebookUrl = "";
        }
    }

    /***
     *
     * @return website URL as string to find further information regarding the event
     */
    public String getWebsiteUrl() {
        return websiteUrl;
    }

    /***
     *
     * @param websiteUrl Optional website URL as string to find further information regarding the event
     */
    public void setWebsiteUrl(String websiteUrl) throws URISyntaxException, MalformedURLException {
        if (Strings.isNotBlank(websiteUrl)) {
            URI i = new URI(websiteUrl);
            URL u = i.toURL();
            this.websiteUrl = u.toString();
        } else {
            this.websiteUrl = "";
        }
    }

    /***
     *
     * @return banner URL to illustrate the event
     */
    public String getBannerUrl() {
        return bannerUrl;
    }

    /***
     *
     * @param bannerUrl Optional banner URL to illustrate the event
     */
    public void setBannerUrl(String bannerUrl) throws URISyntaxException, MalformedURLException {
        if (Strings.isNotBlank(bannerUrl)) {
            URI i = new URI(bannerUrl);
            URL u = i.toURL();
            this.bannerUrl = u.toString();
        } else {
            this.bannerUrl = "";
        }
    }

    /**
     * @return Image filename on the server
     */
    public String getImageFile() {
        return this.imageFile;
    }

    /**
     * @param imageFile Filename of the server
     */
    public void setImageFile(String imageFile) {
        this.imageFile = imageFile;
    }

    /**
     * @return the bannerURL file path only
     */
    @Transient
    public String getBannerUrlAsResource() {
        String resource;
        try {
            URI i = new URI(bannerUrl);
            URL u = i.toURL();
            resource =  u.getPath();
        } catch (IllegalArgumentException | URISyntaxException | MalformedURLException e) {
            resource = "";
        }
        return resource;
    }

    /**
     * @return the bannerURL file path only
     */
    @Transient
    public String getBannerUrlAsFilesystem() {
        String resource;
        try {
            URI i = new URI(bannerUrl);
            URL u = i.toURL();
            resource =  "~" + u.getPath();
        } catch (IllegalArgumentException | URISyntaxException | MalformedURLException e) {
            resource = "";
        }
        return resource;
    }
}