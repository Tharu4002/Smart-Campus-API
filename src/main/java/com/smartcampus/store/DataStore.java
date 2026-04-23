package com.smartcampus.store;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataStore {
    public static final Map<String, Room> ROOMS = new ConcurrentHashMap<>();
    public static final Map<String, Sensor> SENSORS = new ConcurrentHashMap<>();
    public static final Map<String, List<SensorReading>> READINGS = new ConcurrentHashMap<>();

    static {
        Room room = new Room("LIB-301", "Library Quiet Study", 40);
        ROOMS.put(room.getId(), room);
    }

    private DataStore() {
    }

    public static List<SensorReading> getReadingsForSensor(String sensorId) {
        return READINGS.computeIfAbsent(sensorId, k -> new ArrayList<>());
    }
}
