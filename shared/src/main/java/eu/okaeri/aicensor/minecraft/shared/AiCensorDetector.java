/*
 * SklepMC Plugin
 * Copyright (C) 2019 SklepMC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package eu.okaeri.aicensor.minecraft.shared;

import eu.okaeri.aicensor.client.ApiError;
import eu.okaeri.aicensor.client.ApiException;
import eu.okaeri.aicensor.client.CensorApiContext;
import eu.okaeri.aicensor.client.info.CensorPredictionInfo;

public abstract class AiCensorDetector {

    private final CensorApiContext context;

    public AiCensorDetector(CensorApiContext context) {
        this.context = context;
    }

    public CensorApiContext getContext() {
        return this.context;
    }

    public boolean shouldBeBlocked(String message) {

        CensorPredictionInfo prediction;
        try {
            prediction = CensorPredictionInfo.get(this.context, message);
        } catch (ApiException exception) {
            ApiError apiError = exception.getApiError();
            this.warning("Blad komunikacji z API AI.Censor: " + apiError.getType() + ", " + apiError.getMessage());
            return false;
        }

        boolean swear = prediction.getGeneral().isSwear();
        if (swear) {
            this.info("Zablokowano wiadomosc '" + message + "': " + prediction.getGeneral().getBreakdown() + " [ID: " + prediction.getId() + "]");
        }

        return swear;
    }

    public abstract void warning(String message);

    public abstract void info(String message);
}
