package main;


import com.google.gson.internal.LinkedTreeMap;
import db.DBThread;
import db.Database;
import db.DatabaseSetup;
import movements.Moment;
import movements.Movement;
import movements.Option;
import movements.Trade;
import server.ServerHandler;
import util.Printer;
import util.Utility;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import static main.ApiController.CURRENCY.BTC;
import static main.ApiController.CURRENCY.ETH;
import static util.Printer.LOGTYPE.INFO;

public class Starter {

    public static String repo = "";

    private static final double MAX_PRICE_INCREASE_MULIPLIER = 2.5; //= 150% loss
    private static final double HARD_STOP_BTC_LOSS = 0.05;
    private static final double HARD_STOP_ETH_LOSS = 1.0;

    private static ApiController api;
    public static Database db;
    public static String db_schema = "";

    public static HashMap<String, String> userMappingsBTC;
    public static HashMap<String, String> userMappingsETH;

    public static String[] a;

    public static void main(String[] args){
        a = args;

        Printer.checkSetup();
        HashMap<String, String> argmap = Utility.checkStartupArgs(args);
        repo = argmap.get("repo");
        db_schema = argmap.get("dbname");
        Printer.printToLog("Sentry starting up...", INFO);
        db = new Database(argmap.get("user"), argmap.get("pw"), argmap.get("host"), argmap.get("dbname"));
        DatabaseSetup.checkSetup();

        api = new ApiController();
        api.authenticate(new Credentials(repo + "/api.key"));
        ApiController.userMappingsBTC = Movement.getUserMappings(repo + "/users_btc.mapping");
        ApiController.userMappingsETH = Movement.getUserMappings(repo + "/users_eth.mapping");

        Printer.printToLog("Sentry operational!", INFO);

        while(true){
            try{
                TreeMap<Double, Moment> hb = ApiController.compileTradeList(api,true, BTC);
                TreeMap<Double, Moment> he = ApiController.compileTradeList(api,true, ETH);

                Runnable r = new DBThread(hb, he, api.getIndex(ApiController.CURRENCY.BTC), api.getIndex(ApiController.CURRENCY.ETH));
                Thread t = new Thread(r);
                t.start();

                for(Moment moment : hb.values()){
                    if(moment.movement instanceof Trade){
                        if(((Trade) moment.movement).state == Option.STATE.OPEN){
                            evaluatePosition(moment);
                        }
                    }
                }

                for(Moment moment : he.values()){
                    if(moment.movement instanceof Trade){
                        if(((Trade) moment.movement).state == Option.STATE.OPEN){
                            evaluatePosition(moment);
                        }
                    }
                }

                Thread.sleep(500);
            } catch (Exception e){
                Printer.printException(e);
                System.exit(1);
            }
        }
    }

    private static void evaluatePosition(Moment pos) throws Exception {
        Trade t = (Trade) pos.movement;
        if(t.openPos < 0){
            Double index = api.getIndex(t.currency);

            //pos diff == out of money; neg diff == in the money
            double diff = 0.0;

            if(t.kind == Option.KIND.PUT){
                diff = index - t.strikePrice;
            }
            if(t.kind == Option.KIND.CALL){
                diff = t.strikePrice - index;
            }



            LinkedTreeMap map = api.getBookSummary(t.instrumentName);
            double ask = (Double) map.get("ask_price");
            double avgPrem = Math.abs(t.maxGain / t.openPos);
            double maxPrice = avgPrem * MAX_PRICE_INCREASE_MULIPLIER;
            double priceDiff = ask - avgPrem;
            double currPrice = priceDiff * t.openPos;

            DecimalFormat df = new DecimalFormat("#.00");
            DecimalFormat df2 = new DecimalFormat("0.00000");
            Printer.printToLog(t.instrumentName + " diff to strike: " + df.format(diff) + "; MaxPrice: " + df2.format(maxPrice) + " - Ask: " + df2.format(ask) + "; CurrVal: " + df2.format(currPrice), INFO);

            double stopLoss = 1000.0;
            if(t.currency == BTC){
                stopLoss = HARD_STOP_BTC_LOSS;
            } else if(t.currency == ETH){
                stopLoss = HARD_STOP_ETH_LOSS;
            }
            boolean criteriaA = diff < 0;
            boolean criteriaB = maxPrice < ask;
            boolean criteriaC = (currPrice > stopLoss);

            if(criteriaA || criteriaB || criteriaC){
                Printer.printToLog("A: " + criteriaA + ", B: " + criteriaB + ", C: " + criteriaC, INFO);
                Printer.printToLog("Killing " + t.instrumentName, INFO);
                //api.killPosition(t);

            }

        } else {
            Printer.printError("Positive options position supplied, cannot evaluate for option buys!");
        }
    }



    public static HashMap<String, String> getUserMappingByCurrency(ApiController.CURRENCY currency){
        if(currency == BTC){
            return userMappingsBTC;
        } else if(currency == ETH) {
            return userMappingsETH;
        }
        return null;
    }
}
