package lazo.benchmark;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import lazo.index.LazoIndex;
import lazo.sketch.LazoSketch;
import lazo.sketch.SketchType;

public class BasicParallelVersion {

    static int k = 64;
    static int numTasks = 1000;
    static int sizeTask = 1000;
    static Random rand = new Random();
    static BlockingQueue<Task> q = new ArrayBlockingQueue<>(numTasks);

    static class Worker implements Runnable {

	LazoIndex li = new LazoIndex(k);
	boolean goOn = true;

	public void stop() {
	    goOn = false;
	}

	@Override
	public void run() {

	    while (goOn) {
		Task task = null;
		try {
		    task = q.poll(50, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
		if (task != null) {
		    LazoSketch ls = new LazoSketch(k, SketchType.MINHASH);
		    for (Integer i : task.value) {
			ls.update(i.toString());
		    }
		    li.insert(task.key, ls);
		} else {
		    goOn = false;
		}
	    }

	}

    }

    static class Task {
	public int key;
	public List<Integer> value;

	public Task(int key, List<Integer> value) {
	    this.key = key;
	    this.value = value;
	}
    }

    public static void main(String args[]) {

	int numRounds = 10;
	int difThreads = 16;
	for (int numTh = 1; numTh < difThreads; numTh++) {
	    for (int round = 0; round < numRounds; round++) {

		q = new ArrayBlockingQueue<>(numTasks);

		// create set of tasks and fill in queue
		for (int i = 0; i < numTasks; i++) {
		    List<Integer> task = new ArrayList<>();
		    for (int j = 0; j < sizeTask; j++) {
			task.add(rand.nextInt());
		    }
		    q.add(new Task(rand.nextInt(), task));
		}

		// create pool of workers
		int numWorkers = numTh;
		Set<Worker> workers = new HashSet<>();
		Set<Thread> threads = new HashSet<>();
		for (int i = 0; i < numWorkers; i++) {
		    Worker w = new Worker();
		    workers.add(w);
		    threads.add(new Thread(w));
		}
		long start = System.currentTimeMillis();
		for (Thread t : threads) {
		    t.start();
		}
		for (Thread t : threads) {
		    try {
			t.join();
		    } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		    }
		}
		for (Worker w : workers) {
		    w.stop();
		}
		long end = System.currentTimeMillis();
		// System.out.println("#threads: " + numWorkers);
		System.out.println(numTh + "," + (end - start));
	    }
	}

    }

}
