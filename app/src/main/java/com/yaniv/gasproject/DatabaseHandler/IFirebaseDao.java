package com.yaniv.gasproject.DatabaseHandler;

import com.yaniv.gasproject.dm.GasStation;

import java.util.List;

public interface IFirebaseDao {
    void saveToDatabase(List<GasStation> stations);
    List<GasStation> readFromDatabase();
}
