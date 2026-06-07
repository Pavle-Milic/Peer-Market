package mutex;

import app.AppConfig;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class GrindingRoom {

    private static final ConcurrentLinkedQueue<Runnable> jobQueue = new ConcurrentLinkedQueue<>();

    private static final AtomicInteger activeJobs = new AtomicInteger(0);

    private static volatile boolean moreWorkToBeDone = false;

    public static void addToJobQueue(Runnable job) {
        jobQueue.add(job);
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static synchronized void workWorkWorkWorkWorkWork() {

        if (jobQueue.isEmpty()) {
            moreWorkToBeDone = false;
            return;
        }

        AppConfig.timestampedStandardPrint("RadimRadimRadimRadimRadimRadim =========================================");
        activeJobs.incrementAndGet();

        executor.submit(() -> {
            try {
                for (int i = 0; i < 6; i++) {
                    Runnable job = jobQueue.poll();
                    if (job != null) {
                        job.run();
                    } else {
                        break;
                    }
                }
            } finally {
                moreWorkToBeDone = !jobQueue.isEmpty();
                activeJobs.decrementAndGet();
            }
        });
    }

    public static synchronized void work(Runnable job) {
        AppConfig.timestampedStandardPrint("Radim ==============================================================");

        activeJobs.incrementAndGet();

        executor.submit(() -> {
            try {
                job.run();
            } finally {
                activeJobs.decrementAndGet();
            }
        });
    }

    public static void finallyTimeForLeagueOfLegends() {
        executor.shutdown();
    }

    public static boolean isWorking() {
        return activeJobs.get() > 0;
    }

    public static boolean isMoreWorkToBeDone(){
        return moreWorkToBeDone;
    }
}

