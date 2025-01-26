package com.yaniv.gasproject.dao;

import com.yaniv.gasproject.dm.GasStation;

import java.util.List;

public interface IFirebaseDao {
    void saveToDatabase(List<GasStation> stations);
    List<GasStation> readFromDatabase();
}
