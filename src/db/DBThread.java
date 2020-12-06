package db;

import main.Starter;
import movements.Moment;
import movements.Option;
import movements.Trade;
import util.Utility;

import java.util.Date;
import java.util.TreeMap;

public class DBThread implements Runnable {

    private TreeMap<Double, Moment> compiledListBTC;
    private TreeMap<Double, Moment> compiledListETH;
    private double indexBTC;
    private double indexETH;

    public DBThread(TreeMap<Double, Moment> listBTC, TreeMap<Double, Moment> listETH, double iBTC, double iETH){
        compiledListBTC = listBTC;
        compiledListETH = listETH;
        indexBTC = iBTC;
        indexETH = iETH;
    }

    @Override
    public void run() {
        compileCurrBal();
        compileCurrTrades();
    }

    private void compileCurrTrades(){
        Starter.db.runInsert("DELETE FROM " + Starter.db_schema + ".curr_trades WHERE 1=1;");
        insertTrades(compiledListBTC, indexBTC);
        insertTrades(compiledListETH, indexETH);
    }

    private void insertTrades(TreeMap<Double, Moment> compiledList, double index) {
        for(Moment m : compiledList.values()){
            if(m.movement instanceof Trade){
                Trade t = (Trade) m.movement;
                if(t.state == Option.STATE.OPEN){
                    double currValUSD = t.change * index;
                    double maxValUSD = t.maxGain * index;
                    Starter.db.runInsert("INSERT INTO " + Starter.db_schema + ".curr_trades VALUES('" +
                            t.instrumentName + "', '" +
                            t.currency.toString() + "', '" +
                            t.expiryDate.toString() + "', " +
                            t.strikePrice + ", '" +
                            t.kind + "', '" +
                            t.state + "', '" +
                            t.initialRuntime + "', '" +
                            t.timeRemaining + "', '" +
                            t.openPos + "', " +
                            t.change + ", " +
                            currValUSD + ", " +
                            t.maxGain + ", " +
                            maxValUSD + ");");
                }
            }
        }
    }

    private void compileCurrBal(){
        Starter.db.runInsert("DELETE FROM " + Starter.db_schema + ".curr_bal WHERE 1=1;");

        for(String user : compiledListBTC.firstEntry().getValue().userCapitalNew.keySet()){
            String hash = Utility.hashSHA256(user);

            double[] valsBTC = getOpenTradeVals(compiledListBTC);
            double[] valsETH = getOpenTradeVals(compiledListETH);

            Starter.db.runInsert("INSERT INTO " + Starter.db_schema + ".curr_bal VALUES('" +
                    hash + "', " +
                    compiledListBTC.lastEntry().getValue().userCapitalNew.get(user) + ", " +
                    compiledListBTC.lastEntry().getValue().userCapitalNew.get(user) * indexBTC + ", " +
                    valsBTC[0] + ", " +
                    valsBTC[0] * indexBTC + ", " +
                    valsBTC[1] + ", " +
                    valsBTC[1] * indexBTC + ", '" +
                    getLongestExpiryDate(compiledListBTC) + "', " +
                    compiledListETH.lastEntry().getValue().userCapitalNew.get(user) + ", " +
                    compiledListETH.lastEntry().getValue().userCapitalNew.get(user) * indexETH + ", " +
                    valsETH[0] + ", " +
                    valsETH[0] * indexETH + ", " +
                    valsETH[1] + ", " +
                    valsETH[1] * indexETH + ", '" +
                    getLongestExpiryDate(compiledListETH) + "', " +
                    (compiledListBTC.lastEntry().getValue().userCapitalNew.get(user) * indexBTC + compiledListETH.lastEntry().getValue().userCapitalNew.get(user) * indexETH) + ");");
        }
    }

    private double[] getOpenTradeVals(TreeMap<Double, Moment> map){
        double[] results = new double[2]; //0 max val; 1 curr val
        results[0] = 0.0;
        results[1] = 0.0;
        for(Moment m : map.values()){
            if(m.movement instanceof Trade){
                Trade t = (Trade) m.movement;
                if(t.state == Option.STATE.OPEN){
                    results[0] += t.maxGain;
                    results[1] += t.change;
                }
            }
        }
        return results;
    }

    private String getLongestExpiryDate(TreeMap<Double, Moment> map){
        Date newestDate = new Date(0);
        for(Moment m : map.values()){
            if(m.movement instanceof Trade){
                Trade t = (Trade) m.movement;
                if(t.expiryDate.getTime() > newestDate.getTime()){
                    newestDate = t.expiryDate;
                }
            }
        }
        return newestDate.toString();
    }
}