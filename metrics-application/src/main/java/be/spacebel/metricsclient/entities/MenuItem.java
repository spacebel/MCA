/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.spacebel.metricsclient.entities;

/**
 * This class represents a menu item that is displayed on the banner
 * 
 * @author mng
 */
public class MenuItem {

    private final String text;
    private final String url;

    public MenuItem(String text, String url) {
        this.text = text;
        this.url = url;
    }

    public String getText() {
        return text;
    }

    public String getUrl() {
        return url;
    }

}
