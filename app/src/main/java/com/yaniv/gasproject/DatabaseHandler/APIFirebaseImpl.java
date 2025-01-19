package com.yaniv.gasproject.DatabaseHandler;

import com.yaniv.gasproject.dm.GasStation;

import java.util.Collections;
import java.util.List;

public class APIFirebaseImpl implements IFirebaseDao{
    @Override
    public void saveToDatabase(List<GasStation> stations) {

    }

    @Override
    public List<GasStation> readFromDatabase() {
        return Collections.emptyList();
    }
}
