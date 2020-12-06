package main;


import com.google.gson.internal.LinkedTreeMap;
import movements.Moment;
import movements.Movement;
import movements.Option;
import movements.Trade;
import server.ServerHandler;
import util.Printer;
import util.Utility;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import static util.Printer.LOGTYPE.INFO;

public class Starter {

    public static String repo = "";

    private static final double MAX_PRICE_INCREASE_MULIPLIER = 2; //= 100% loss
    private static final double HARD_STOP_BTC_LOSS = 0.05;

    private static ApiController api;

    public static HashMap<String, String> userMappingsBTC;
    public static HashMap<String, String> userMappingsETH;

    public static String[] a;

    public static void main(String[] args){
        a = args;

        Printer.checkSetup();
        repo = Utility.checkStartupArgs(args);
        Printer.printToLog("Sentry starting up...", INFO);


        api = new ApiController();
        api.authenticate(new Credentials(repo + "/api.key"));
        ApiController.userMappingsBTC = Movement.getUserMappings(repo + "/users_btc.mapping");
        ApiController.userMappingsETH = Movement.getUserMappings(repo + "/users_eth.mapping");

        Printer.printToLog("Sentry operational!", INFO);

        while(true){
            try{
                TreeMap<Double, Moment> hb = ApiController.compileTradeList(api,true, ApiController.CURRENCY.BTC);
                TreeMap<Double, Moment> he = ApiController.compileTradeList(api,true, ApiController.CURRENCY.ETH);


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

            Printer.printToLog(t.instrumentName + " diff to strike: " + diff, INFO);

            LinkedTreeMap map = api.getBookSummary(t.instrumentName);
            double ask = (Double) map.get("ask_price");
            double avgPrem = Math.abs(t.getChange() / t.openPos);
            double maxPrice = avgPrem * MAX_PRICE_INCREASE_MULIPLIER;
            double priceDiff = ask - avgPrem;
            double currPrice = Math.abs(priceDiff * t.openPos);

            boolean criteriaA = diff < 0;
            boolean criteriaB = maxPrice < ask;
            boolean criteriaC = (currPrice > HARD_STOP_BTC_LOSS) && (priceDiff > 0);

            if(criteriaA || criteriaB || criteriaC){
                Printer.printToLog("A: " + criteriaA + ", B: " + criteriaB + ", C: " + criteriaC, INFO);
                Printer.printToLog("Killing " + t.instrumentName, INFO);
                api.killPosition(t);

            }

        } else {
            Printer.printError("Positive options position supplied, cannot evaluate for option buys!");
        }
    }



    public static HashMap<String, String> getUserMappingByCurrency(ApiController.CURRENCY currency){
        if(currency == ApiController.CURRENCY.BTC){
            return userMappingsBTC;
        } else if(currency == ApiController.CURRENCY.ETH) {
            return userMappingsETH;
        }
        return null;
    }
}
