package mutex;

import app.AppConfig;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GrindingRoom {

    private static final ConcurrentLinkedQueue<Runnable> jobQueue = new ConcurrentLinkedQueue<>();

    private static volatile boolean working=false;

    public static void addToJobQueue(Runnable job) {
        jobQueue.add(job);
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void workWorkWorkWorkWorkWork() {
        AppConfig.timestampedStandardPrint("RadimRadimRadimRadimRadimRadim =========================================");
        working=true;
        executor.submit(() -> {
            for (int i = 0; i < 6; i++) {
                Runnable job = jobQueue.poll();

                if (job != null) {
                    job.run();
                } else {
                    break;
                }
            }
        });
        working=false;
    }

    public static void work(Runnable job){
        AppConfig.timestampedStandardPrint("Radim ==============================================================");

        working=true;
        executor.submit(job);
        job.run();
        working=false;
    }

    public static void finallyTimeForLeagueOfLegends() {
        executor.shutdown();
    }

    public static boolean isWorking() {
        return working;
    }
}

