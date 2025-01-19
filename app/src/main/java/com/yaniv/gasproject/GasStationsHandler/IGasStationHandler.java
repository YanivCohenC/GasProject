package com.yaniv.gasproject.GasStationsHandler;

import com.yaniv.gasproject.dm.GasStation;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public interface IGasStationHandler {
    List<GasStation> fetchGasStations(String query, String type);

    static String sendHTTPRequest(String query) {
        // Making a GET request with OkHttp
        OkHttpClient httpClient = new OkHttpClient.Builder().build();
        Request request = new Request.Builder()
                .url(query)
                .build();
        Call call = httpClient.newCall(request);
        CompletableFuture<String> future = new CompletableFuture<>();
        call.enqueue(new Callback() {
            public void onResponse(Call call, Response response)
                    throws IOException {
                try {
                    if (response.isSuccessful()) {
                        future.complete(response.body().string());
                    } else {
                        future.completeExceptionally(new IOException("Request failed: " + response.code()));
                    }
                } catch (IOException e) {
                    future.completeExceptionally(e);
                }
            }

            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }
        });
        try {
            String res = future.get();
            return res;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
