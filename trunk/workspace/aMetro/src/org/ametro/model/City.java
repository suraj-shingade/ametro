/*
 * http://code.google.com/p/ametro/
 * Transport map viewer for Android platform
 * Copyright (C) 2009-2010 Roman.Golovanov@gmail.com and other
 * respective project committers (see project home page)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.ametro.model;

/**
 * @author Vlad Vinichenko (akerigan@gmail.com)
 *         Date: 11.02.2010
 *         Time: 22:15:14
 */
public class City {

    public static final int VERSION = 1;

    public int id;

    public String countryName;
    public String cityName;

    public int width;
    public int height;

    public long timestamp;
    public long crc;

    public long renderVersion;
    public long sourceVersion;

    public SubwayMap subwayMap;

    public boolean completeEqual(City other) {
        return locationEqual(other)
                && crc == other.crc
                && timestamp == other.timestamp
                && sourceVersion == other.sourceVersion;
    }

    public boolean locationEqual(City other) {
        return countryName.equals(other.countryName)
                && cityName.equals(other.cityName);
    }
}