/*
    Copyright(c) 2019 AuroraLS3

    The MIT License(MIT)

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files(the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions :
    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
*/
package net.playeranalytics.extension.towny;

import com.djrapitops.plan.extension.DataExtension;
import com.djrapitops.plan.extension.Group;
import com.djrapitops.plan.extension.NotReadyException;
import com.djrapitops.plan.extension.annotation.GroupProvider;
import com.djrapitops.plan.extension.annotation.NumberProvider;
import com.djrapitops.plan.extension.annotation.PluginInfo;
import com.djrapitops.plan.extension.annotation.StringProvider;
import com.djrapitops.plan.extension.icon.Color;
import com.djrapitops.plan.extension.icon.Family;
import com.djrapitops.plan.settings.SettingsService;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Towny DataExtension.
 *
 * @author AuroraLS3
 */
@PluginInfo(name = "Towny", iconName = "university", iconFamily = Family.SOLID, color = Color.BROWN)
public class TownyExtension implements DataExtension {

    public TownyExtension() {
    }

    private Resident getResident(String playerName) {
        try {
            Resident resident = TownyAPI.getInstance().getResident(playerName);
            if (resident == null) {
                throw new NotReadyException();
            }
            return resident;
        } catch (RuntimeException e) {
            throw new NotReadyException();
        }
    }

    private Optional<Town> getTown(String playerName) {
        Resident resident = getResident(playerName);
        try {
            if (resident.hasTown()) {
                List<String> ignoredTowns = getIgnoredTowns();
                Town town = resident.getTown();
                if (ignoredTowns.contains(town.getName())) {
                    return Optional.empty();
                }

                return Optional.of(town);
            }
            return Optional.empty();
        } catch (NotRegisteredException doesNotHaveTown) {
            return Optional.empty();
        }
    }

    private List<String> getIgnoredTowns() {
        return SettingsService.getInstance().getStringList("Towny.HideTowns", () -> Collections.singletonList("ExampleTown"));
    }

    @GroupProvider(text = "Town", groupColor = Color.BROWN, iconName = "university")
    public String[] town(String playerName) {
        return getTown(playerName).map(Town::getName).map(name -> new String[]{name}).orElse(new String[0]);
    }

    @StringProvider(
            text = "Mayor",
            description = "Who runs the town",
            iconName = "user",
            iconColor = Color.BROWN,
            priority = 100,
            playerName = true
    )
    public String townMayor(Group townName) {
        return getTown(townName).getMayor().getName();
    }

    @StringProvider(
            text = "Town Coordinates",
            description = "Where is the town located",
            iconName = "map-pin",
            iconColor = Color.RED,
            priority = 95
    )
    public String townCoordinates(Group townName) {
        return getHomeBlock(townName);
    }

    private String getHomeBlock(Group townName) {
        try {
            Coord coord = getTown(townName).getHomeBlock().getCoord();
            return "x: " + coord.getX() + " z: " + coord.getZ();
        } catch (TownyException e) {
            return "Not Set";
        }
    }

    private Town getTown(Group townName) {
        try {
            return TownyAPI.getInstance().getTown(townName.getGroupName());
        } catch (RuntimeException e) {
            throw new NotReadyException();
        }
    }

    @NumberProvider(
            text = "Number of Towns",
            description = "How many towns there are",
            priority = 105,
            iconName = "university",
            iconColor = Color.BROWN
    )
    public long numberOfTowns() {
        List<String> ignoredTowns = getIgnoredTowns();
        return TownyAPI.getInstance().getDataSource().getTowns()
                .stream()
                .map(Town::getName)
                .filter(townName -> !ignoredTowns.contains(townName))
                .count();
    }

    @StringProvider(
            text = "Nation",
            description = "What nation is the town part of",
            priority = 90,
            iconName = "flag",
            iconColor = Color.BROWN
    )
    public String nation(Group townName) {
        return getNation(townName).map(Nation::getName).orElse("None");
    }

    private Optional<Nation> getNation(Group townName) {
        try {
            Town town = getTown(townName);
            if (town.hasNation()) {
                return Optional.of(town.getNation());
            }
            return Optional.empty();
        } catch (NotRegisteredException e) {
            return Optional.empty();
        }
    }
}