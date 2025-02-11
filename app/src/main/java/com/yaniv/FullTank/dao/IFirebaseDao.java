package com.yaniv.FullTank.dao;

import com.yaniv.FullTank.dm.GasStation;

import java.util.List;

public interface IFirebaseDao {
    void saveToDatabase(List<GasStation> stations);
    List<GasStation> readFromDatabase();
}
