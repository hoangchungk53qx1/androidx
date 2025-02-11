/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.core.location.altitude.impl.db;

import androidx.core.location.altitude.impl.AltitudeConverter;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import org.jspecify.annotations.NonNull;

/** Defines the resource database for {@link AltitudeConverter}. */
@Database(entities = {MapParamsEntity.class, TilesEntity.class}, version = 1, exportSchema = false)
@TypeConverters({MapParamsEntity.class, TilesEntity.class})
public abstract class AltitudeConverterDatabase extends RoomDatabase {

    /** Returns the data access object for the MapParams table. */
    public abstract @NonNull MapParamsDao mapParamsDao();

    /** Returns the data access object for the Tiles table. */
    public abstract @NonNull TilesDao tilesDao();
}
