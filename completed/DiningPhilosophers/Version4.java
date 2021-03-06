import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

enum States
{
    THINKING, HUNGRY, EATING
}

public class Version4
{
    public static void main(String[] args)
    {
        System.out.println("Press 'ctrl + c' to quit");
        int numPhilosophers, numChopsticks;
        
        try {
            numPhilosophers = numChopsticks = (args.length == 1) ? Integer.parseInt(args[0]) : 5;
        } catch (NumberFormatException e) {
            System.out.println("the value you entered wasn't an integer. Setting numPhilosophers to 5.");
            numPhilosophers = numChopsticks = 5;
        }

        if (numPhilosophers == 1)
            numChopsticks = 2;

        States[] state = new States[numPhilosophers];
        Arrays.fill(state, States.THINKING);

        // create thread pool, philosophers, and chopsticks
        // number of philosophers must match number of chopsticks
        ExecutorService threadPool = Executors.newCachedThreadPool();
        
        // honestly just created philosophers array for debugging purposes, the
        // code will still work without it
        Philosopher[] philosophers = new Philosopher[numPhilosophers];
        Chopstick[] chopsticks = new Chopstick[numChopsticks];

        // instantiate and assign each chopstick with a unique id
        for (int i = 0; i < numChopsticks; i++)
        {
            chopsticks[i] = new Chopstick(i);
        }

        for (int i = 0; i < numPhilosophers;i++)
        {
            threadPool.execute(philosophers[i] = 
            new Philosopher(chopsticks[i],
                            chopsticks[(i + 1) % numPhilosophers],
                            i, numPhilosophers, state));
        }

        try {
            System.in.read();
        } catch (IOException e) {
           System.out.println("invalid input");
        }
        finally
        {
            threadPool.shutdownNow();
        }
    }
}

class Chopstick
{
    private boolean inUse = false;
    private int id;
    private static ReentrantLock lock = new ReentrantLock(true);

    public Chopstick(int id)
    {
        this.id = id;
    }

    public void PickUp() throws InterruptedException
    {
        lock.lock();
        inUse = true;
        System.out.println("chopstick " + id + " was picked up.");
        lock.unlock();
    }

    public void PutDown() throws InterruptedException
    {
        lock.lock();
        inUse = false;
        System.out.println("chopstick " + id + " was put down.");
        lock.unlock();
    }

    public synchronized boolean InUse()
    {
        return inUse;
    }

    public synchronized int GetChopstickId()
    {
        return id;
    }
}

// assume all philosophers wait and eat at indefinitely
class Philosopher implements Runnable
{
    // 2 states: Thinking or Eating
    private Chopstick left, right;
    private int id;
    private Random rnd;
    private int numPhilosophers;
    private States[] state;

    public Philosopher(Chopstick left, Chopstick right, int id, int numPhilosophers, States[] state)
    {
        this.left = left;
        this.right = right;
        this.id = id;
        rnd = new Random(47);
        this.numPhilosophers = numPhilosophers;
        this.state = state;
    }

    private synchronized void Eating() throws InterruptedException
    {
        System.out.println(this + " is now eating");
        TimeUnit.MILLISECONDS.sleep(
                rnd.nextInt(2500));
    }

    // this is where all the magic happens with each philosopher
    @Override
    public void run()
    {
        try {
            while (true)
            {
                // pick up both chopsticks if neighbors aren't
                // eating and if both chopsticks aren't in use
                if (!left.InUse() && !right.InUse()
                    && state[(id + 1) % numPhilosophers] != States.EATING
                    && state[(id + numPhilosophers - 1) % numPhilosophers] != States.EATING)
                {
                    left.PickUp();
                    right.PickUp();
                    // because the philosopher picked up both
                    // chopsticks, he/she must be hungry
                    state[id] = States.HUNGRY;
                    System.out.println(this + " is now hungry");
                }
                
                // if neighbors aren't eating and you're hungry
                if (state[(id + 1) % numPhilosophers] != States.EATING
                    && state[(id + numPhilosophers - 1) % numPhilosophers] != States.EATING
                    && state[id] == States.HUNGRY)
                {
                    // BON APPETITE
                    state[id] = States.EATING;
                    Eating();
                }

                // if you're eating, then you're probably full by now,
                // so it's time to put BOTH chopsticks down and start
                // thinking again
                if (state[id] == States.EATING)
                {
                    left.PutDown();
                    right.PutDown();
                    state[id] = States.THINKING;
                    System.out.println(this + " is now thinking");
                }
                
            }
        } catch (InterruptedException e) {
            System.out.println(this + " exited via interruption");
        }
    }

    @Override
    public String toString() {
        return "Philosopher " + id;
    }
}
