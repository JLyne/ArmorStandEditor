/*
 * ArmorStandEditor: Bukkit plugin to allow editing armor stand attributes
 * Copyright (C) 2016-2023  RypoFalem
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package io.github.rypofalem.armorstandeditor.modes;

import org.bukkit.permissions.Permissible;

public enum EditMode {
    NONE(null),
    VISIBILITY("asedit.togglearmorstandvisibility"),
    SHOWARMS("asedit.togglearms"),
    GRAVITY("asedit.togglegravity"),
    BASEPLATE("asedit.togglebaseplate"),
    SIZE("asedit.togglesize"),
    COPY("asedit.copy"),
    PASTE("asedit.paste"),
    HEAD("asedit.basic"),
    BODY("asedit.basic"),
    LEFTARM("asedit.basic"),
    RIGHTARM("asedit.basic"),
    LEFTLEG("asedit.basic"),
    RIGHTLEG("asedit.basic"),
    PLACEMENT("asedit.movement"),
    DISABLESLOTS("asedit.disableslots"),
    ROTATE("asedit.rotation"),
    EQUIPMENT("asedit.equipment"),
    PRESET("asedit.basic"),
    RESET("asedit.reset"),
    ITEMFRAMEVISIBILITY("asedit.toggleitemframevisibility"),
    VULNERABILITY("asedit.toggleInvulnerability"),
    GLOW("asedit.togglearmorstandglow");

    private final String permission;

    EditMode(String permission) {
        this.permission = permission;
    }

    public String toString() {
        return name();
    }

    public boolean hasPermission(Permissible permissible) {
        return permission == null || permissible.hasPermission(permission);
    }
}
