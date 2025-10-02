package org.itsuka.web.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能监控拦截器
 * 监控API调用性能、统计调用次数和响应时间
 * 
 * @author Itsuka
 */
@Slf4j
public class PerformanceMonitorInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTR = "PERF_START_TIME";
    
    // 性能统计数据
    private static final ConcurrentHashMap<String, ApiStats> apiStatsMap = new ConcurrentHashMap<>();
    
    // 慢接口阈值（毫秒）
    private final long slowApiThreshold;
    
    public PerformanceMonitorInterceptor() {
        this(1000); // 默认1秒
    }
    
    public PerformanceMonitorInterceptor(long slowApiThreshold) {
        this.slowApiThreshold = slowApiThreshold;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                           @NonNull HttpServletResponse response,
                           @NonNull Object handler) {
        
        long startTime = System.currentTimeMillis();
        request.setAttribute(START_TIME_ATTR, startTime);
        
        if (handler instanceof HandlerMethod handlerMethod) {
            String apiKey = getApiKey(handlerMethod);
            log.debug("API call started: {} at {}", apiKey, startTime);
        }
        
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                              @NonNull HttpServletResponse response,
                              @NonNull Object handler,
                              Exception ex) {
        
        Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
        if (startTime == null) {
            return;
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        if (handler instanceof HandlerMethod handlerMethod) {
            String apiKey = getApiKey(handlerMethod);
            int statusCode = response.getStatus();
            
            // 更新统计信息
            updateApiStats(apiKey, duration, statusCode, ex != null);
            
            // 记录性能信息
            logPerformance(apiKey, duration, statusCode, ex);
        }
    }

    /**
     * 生成API唯一标识
     */
    private String getApiKey(HandlerMethod handlerMethod) {
        Method method = handlerMethod.getMethod();
        Class<?> clazz = method.getDeclaringClass();
        return clazz.getSimpleName() + "." + method.getName();
    }

    /**
     * 更新API统计信息
     */
    private void updateApiStats(String apiKey, long duration, int statusCode, boolean hasException) {
        apiStatsMap.compute(apiKey, (key, stats) -> {
            if (stats == null) {
                stats = new ApiStats(key);
            }
            stats.updateStats(duration, statusCode, hasException);
            return stats;
        });
    }

    /**
     * 记录性能信息
     */
    private void logPerformance(String apiKey, long duration, int statusCode, Exception ex) {
        ApiStats stats = apiStatsMap.get(apiKey);
        
        if (duration > slowApiThreshold) {
            log.warn("SLOW API detected - {}: {}ms (avg: {}ms, calls: {})", 
                    apiKey, duration, stats.getAverageResponseTime(), stats.getTotalCalls());
        } else {
            log.debug("API completed - {}: {}ms (status: {})", apiKey, duration, statusCode);
        }
        
        if (ex != null) {
            log.error("API error - {}: {} - {}", apiKey, ex.getClass().getSimpleName(), ex.getMessage());
        }
        
        // 每1000次调用打印一次统计信息
        if (stats.getTotalCalls() % 1000 == 0) {
            log.info("API Stats - {}: calls={}, avg={}ms, min={}ms, max={}ms, errors={}, errorRate={}%",
                    apiKey,
                    stats.getTotalCalls(),
                    stats.getAverageResponseTime(),
                    stats.getMinResponseTime(),
                    stats.getMaxResponseTime(),
                    stats.getErrorCount(),
                    String.format("%.2f", stats.getErrorRate()));
        }
    }

    /**
     * 获取所有API统计信息
     */
    public static ConcurrentHashMap<String, ApiStats> getAllApiStats() {
        return new ConcurrentHashMap<>(apiStatsMap);
    }

    /**
     * 获取指定API统计信息
     */
    public static ApiStats getApiStats(String apiKey) {
        return apiStatsMap.get(apiKey);
    }

    /**
     * 重置统计信息
     */
    public static void resetStats() {
        apiStatsMap.clear();
    }

    /**
     * 重置指定API统计信息
     */
    public static void resetStats(String apiKey) {
        apiStatsMap.remove(apiKey);
    }

    /**
     * API统计信息类
     */
    public static class ApiStats {
        private final String apiKey;
        private final AtomicLong totalCalls = new AtomicLong(0);
        private final AtomicLong totalResponseTime = new AtomicLong(0);
        private final AtomicLong minResponseTime = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxResponseTime = new AtomicLong(0);
        private final AtomicLong errorCount = new AtomicLong(0);
        private final AtomicLong successCount = new AtomicLong(0);
        private volatile long lastCallTime = System.currentTimeMillis();

        public ApiStats(String apiKey) {
            this.apiKey = apiKey;
        }

        public synchronized void updateStats(long responseTime, int statusCode, boolean hasException) {
            totalCalls.incrementAndGet();
            totalResponseTime.addAndGet(responseTime);
            lastCallTime = System.currentTimeMillis();

            // 更新最小响应时间
            minResponseTime.updateAndGet(current -> Math.min(current, responseTime));
            
            // 更新最大响应时间
            maxResponseTime.updateAndGet(current -> Math.max(current, responseTime));

            // 统计成功和错误次数
            if (hasException || statusCode >= 400) {
                errorCount.incrementAndGet();
            } else {
                successCount.incrementAndGet();
            }
        }

        public String getApiKey() {
            return apiKey;
        }

        public long getTotalCalls() {
            return totalCalls.get();
        }

        public long getAverageResponseTime() {
            long calls = totalCalls.get();
            return calls > 0 ? totalResponseTime.get() / calls : 0;
        }

        public long getMinResponseTime() {
            long min = minResponseTime.get();
            return min == Long.MAX_VALUE ? 0 : min;
        }

        public long getMaxResponseTime() {
            return maxResponseTime.get();
        }

        public long getErrorCount() {
            return errorCount.get();
        }

        public long getSuccessCount() {
            return successCount.get();
        }

        public double getErrorRate() {
            long total = getTotalCalls();
            return total > 0 ? (double) getErrorCount() / total * 100 : 0.0;
        }

        public double getSuccessRate() {
            long total = getTotalCalls();
            return total > 0 ? (double) getSuccessCount() / total * 100 : 0.0;
        }

        public long getLastCallTime() {
            return lastCallTime;
        }

        @Override
        public String toString() {
            return String.format("ApiStats{api='%s', calls=%d, avg=%dms, min=%dms, max=%dms, errors=%d, errorRate=%.2f%%}",
                    apiKey, getTotalCalls(), getAverageResponseTime(), getMinResponseTime(), 
                    getMaxResponseTime(), getErrorCount(), getErrorRate());
        }
    }
}