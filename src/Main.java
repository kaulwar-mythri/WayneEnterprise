import java.util.*;
import java.util.concurrent.*;

class Order {
    long timeOfCreation;

    int cargoWeight;

    String dest;

    Random rand = new Random();

    public Order() {
        this.timeOfCreation = System.currentTimeMillis();
        this.cargoWeight = rand.nextInt(41) + 10;
        this.dest = rand.nextBoolean() ? "Atlanta" : "Gotham";
    }
}

class Ship {
    private WayneEnterprise wp;
    final int minCargo = 50;
    final int maxCargo = 300;
    final int orderProfit = 1000;
    final int cancellationSetback = 250;
    static long maxWaitingTime = 60000;

    String dest;
    int totalTrips;
    int totalCargoWeight;
    Random rand = new Random();
    public Ship(WayneEnterprise wp) {
        this.wp = wp;
        dest = rand.nextBoolean() ? "Atlanta" : "Gotham";
        totalTrips = 0;
        totalCargoWeight = 0;
    }

    public void pickUpOrder(Order order) {
        if(wp.totalIncome >= wp.MAX_INCOME)
            return;

        if(System.currentTimeMillis() - order.timeOfCreation > 60000) {
            if(order.dest.equals("Atlanta")) {
                wp.AtlantaorderDQ.remove(order);
            } else {
                wp.GothamorderDQ.remove(order);
            }

            synchronized (this) {
                wp.totalIncome -= 250;
                wp.totalCancelled++;
            }
            return;
        }

        if(totalCargoWeight + order.cargoWeight < maxCargo) {
            totalCargoWeight += order.cargoWeight;
        }

        synchronized (this) {
            wp.totalDelivered++;
            wp.totalIncome += 1000;
        }

        if(totalCargoWeight > minCargo) {
            totalTrips++;
            totalCargoWeight = 0;

            dest = dest.equals("Atlanta") ? "Gotham" : "Atlanta";

            if(totalTrips % 5 == 0) {
                System.out.println("Ship under maintanance");

                try {
                    Thread.sleep(60000);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

class ShippingThread implements Runnable {
    private WayneEnterprise wp;
    private Ship ship;
    public ShippingThread(Ship ship, WayneEnterprise wp) {
        this.ship = ship;
        this.wp = wp;
    }
    @Override
    public void run() {
        while(wp.totalIncome < wp.MAX_INCOME) {
            try {
                if(ship.dest.equals("Atlanta")) {
                    Order o = wp.AtlantaorderDQ.take();
                    ship.pickUpOrder(o);
                } else {
                    Order o = wp.GothamorderDQ.take();
                    ship.pickUpOrder(o);
                }
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Total orders delivered: " + wp.totalDelivered);
        System.out.println("Total orders cancelled: " + wp.totalCancelled);
    }
}

class ConsumerThread implements Runnable {
    private WayneEnterprise wp;

    public ConsumerThread(WayneEnterprise wp) {
        this.wp = wp;
    }
    @Override
    public void run() {
        while(wp.totalIncome < wp.MAX_INCOME) {
            try {
                Order order = new Order();
                if(order.dest == "Atlanta")
                    wp.AtlantaorderDQ.add(order);
                else
                    wp.GothamorderDQ.add(order);

                Thread.sleep(6000);
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

class WayneEnterprise {
    BlockingDeque<Order> AtlantaorderDQ;   //dest is Atlanta
    BlockingDeque<Order> GothamorderDQ;   //dest is Gotham
    int totalIncome = 0;
    int totalDelivered = 0;
    int totalCancelled = 0;
    final int MAX_INCOME = 1000000;

    public WayneEnterprise() {
        AtlantaorderDQ = new LinkedBlockingDeque<>();
        GothamorderDQ = new LinkedBlockingDeque<>();
    }
}


public class Main {
    static Ship[] ships = new Ship[5];
    public static void main(String[] args) {
        WayneEnterprise wp = new WayneEnterprise();

        ExecutorService shippingPool = Executors.newFixedThreadPool(5);
        ExecutorService consumerPool = Executors.newFixedThreadPool(7);

        for(int i=0; i<7; i++) {
            consumerPool.submit(new ConsumerThread(wp));
        }

        for(int i=0; i<5; i++) {
            ships[i] = new Ship(wp);
            shippingPool.submit(new ShippingThread(ships[i], wp));
        }

        shippingPool.shutdown();
        consumerPool.shutdown();
    }
}