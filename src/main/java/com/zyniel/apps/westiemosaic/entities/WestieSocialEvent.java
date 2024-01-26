package com.zyniel.apps.westiemosaic.entities;

import jakarta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Objects;
import java.util.StringJoiner;

@Entity
@DiscriminatorValue("social")
public class WestieSocialEvent extends WestieEvent {

    private static final Logger LOGGER = LoggerFactory.getLogger(WestieSocialEvent.class);

    private WestieSocialEvent() {
        super();
    }

    /***
     * West Coast Swing non-competitive Social event
     * @param name Name of the event
     * @param startDate Day on which the event starts. Must be prior or equal to the end date.
     * @param endDate Day on which the event ends. Must be later or equal to the start date.
     * @param city Name of the city hosting the event
     * @param country Name of the country hosting the event
     */
    public WestieSocialEvent(String name, Date startDate, Date endDate, String city, String country) {
        super(name, startDate, endDate, city, country);
        super.setCompetitive(false); // Not a competitive event
    }

    /***
     * West Coast Swing non-competitive Social event with all information
     * @param name Name of the event
     * @param startDate Day on which the event starts. Must be prior or equal to the end date.
     * @param endDate Day on which the event ends. Must be later or equal to the start date.
     * @param city Name of the city hosting the event
     * @param country Name of the country hosting the event
     * @param fullLocation Aggregation of all location information (Venue, Address, City, Country)
     * @param facebookUrl External facebook URL to get further information about the event
     * @param websiteUrl External website URL to get further information about the event
     * @param imageFile Name of the image file stored on filesystem
     * @param bannerUrl External banner URL to illustrate the event
     */
    public WestieSocialEvent(String name, Date startDate, Date endDate, String city, String country, String fullLocation, String facebookUrl, String websiteUrl, String bannerUrl, String imageFile) {
        this(name, startDate, endDate, city, country);
        super.setFullLocation(fullLocation);
        super.setImageFile(imageFile);

        try {
            super.setFacebookUrl(facebookUrl);
            super.setWebsiteUrl(websiteUrl);
            super.setBannerUrl(bannerUrl);
        } catch (URISyntaxException | MalformedURLException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WestieSocialEvent event = (WestieSocialEvent) o;
        return Objects.equals(super.id, event.id) &&
                Objects.equals(super.name, event.name) &&
                Objects.equals(super.country, event.country) &&
                Objects.equals(super.city, event.city) &&
                Objects.equals(super.fullLocation, event.fullLocation) &&
                Objects.equals(super.startDate, event.startDate) &&
                Objects.equals(super.endDate, event.endDate) &&
                Objects.equals(super.websiteUrl, event.websiteUrl) &&
                Objects.equals(super.bannerUrl, event.bannerUrl) &&
                Objects.equals(super.facebookUrl, event.facebookUrl) &&
                Objects.equals(super.imageFile, event.imageFile) &&
                super.isWSDC == event.isWSDC;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, country, city, fullLocation, startDate, endDate, websiteUrl, bannerUrl, facebookUrl, imageFile, isWSDC);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", WestieSocialEvent.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("name='" + name + "'")
                .add("country='" + country + "'")
                .add("city='" + city + "'")
                .add("fullLocation='" + fullLocation + "'")
                .add("startDate='" + startDate.toString() + "'")
                .add("endDate='" + endDate.toString() + "'")
                .add("websiteUrl='" + websiteUrl + "'")
                .add("bannerUrl='" + bannerUrl + "'")
                .add("facebookUrl='" + facebookUrl + "'")
                .add("imageFile='" + imageFile + "'")
                .add("isWSDC='" + isWSDC + "'")
                .toString();
    }

}