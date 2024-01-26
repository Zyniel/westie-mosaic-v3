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
@DiscriminatorValue("wsdc")
public class WestieWSDCEvent extends WestieEvent {

    private WestieWSDCEvent() {
        super();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(WestieWSDCEvent.class);

    /***
     * West Coast Swing competitive WSDC event
     * @param name Name of the event
     * @param startDate Day on which the event starts. Must be prior or equal to the end date.
     * @param endDate Day on which the event ends. Must be later or equal to the start date.
     * @param city Name of the city hosting the event
     * @param country Name of the country hosting the event
     */
    public WestieWSDCEvent(String name, Date startDate, Date endDate, String city, String country) {
        super(name, startDate, endDate, city, country);
        // Competitive event
        super.setCompetitive(true);
    }

    /***
     * West Coast Swing competitive WSDC event with all information
     * @param name Name of the event
     * @param startDate Day on which the event starts. Must be prior or equal to the end date.
     * @param endDate Day on which the event ends. Must be later or equal to the start date.
     * @param city Name of the city hosting the event
     * @param country Name of the country hosting the event
     * @param fullLocation Aggregation of all location information (Venue, Address, City, Country)
     * @param facebookUrl External facebook URL as string to get further information about the event
     * @param websiteUrl External website URL as string to get further information about the event
     * @param bannerUrl External banner URL as string to illustrate the event
     */
    public WestieWSDCEvent(String name, Date startDate, Date endDate, String city, String country, String fullLocation, String facebookUrl, String websiteUrl, String bannerUrl, String imageFile) {
        this(name, startDate, endDate, city, country);
        // Competitive event
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
        WestieWSDCEvent event = (WestieWSDCEvent) o;
        return Objects.equals(super.id, event.id) &&
                Objects.equals(super.name, event.name) &&
                Objects.equals(super.country, event.country) &&
                Objects.equals(super.city, event.city) &&
                Objects.equals(super.fullLocation, event.fullLocation) &&
                Objects.equals(super.startDate, event.startDate) &&
                Objects.equals(super.endDate, event.endDate) &&
                Objects.equals(super.websiteUrl, event.websiteUrl) &&
                Objects.equals(super.bannerUrl, event.bannerUrl) &&
                Objects.equals(super.imageFile, event.imageFile) &&
                Objects.equals(super.facebookUrl, event.facebookUrl) &&
                super.isWSDC == event.isWSDC;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, country, city, fullLocation, startDate, endDate, websiteUrl, bannerUrl, facebookUrl, imageFile, isWSDC);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", WestieWSDCEvent.class.getSimpleName() + "[", "]")
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