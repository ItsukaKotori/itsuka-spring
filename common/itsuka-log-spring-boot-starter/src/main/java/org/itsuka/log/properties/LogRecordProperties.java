package org.itsuka.log.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.regex.Pattern;

/**
 * 操作日志配置属性。
 *
 * @author Itsuka
 */
@Data
@ConfigurationProperties(prefix = "itsuka.log")
public class LogRecordProperties {

    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]{0,63}$");

    /**
     * 是否启用操作日志
     */
    private boolean enabled = true;

    /**
     * 存储方式
     */
    private Storage storage = Storage.JDBC;

    /**
     * 表名，只允许字母、数字、下划线
     */
    private String tableName = "itsuka_operation_log";

    /**
     * 启动时是否执行建表脚本
     */
    private boolean autoInitSchema = true;

    /**
     * 建表脚本位置，支持 ${tableName} 占位符
     */
    private String schemaLocation = "classpath:META-INF/itsuka/log/schema-mysql.sql";

    /**
     * content 截断长度
     */
    private int contentMaxLength = 2000;

    /**
     * detail 截断长度
     */
    private int detailMaxLength = 4000;

    /**
     * errorMsg 截断长度
     */
    private int errorMaxLength = 2000;

    /**
     * 切面顺序，默认在事务切面外层
     */
    private int aspectOrder = Integer.MAX_VALUE - 1;

    /**
     * 异步写入配置
     */
    private Async async = new Async();

    /**
     * 操作人配置
     */
    private Operator operator = new Operator();

    /**
     * 请求信息配置
     */
    private Request request = new Request();

    /**
     * 事务配置
     */
    private Transaction transaction = new Transaction();

    /**
     * 查询配置
     */
    private Query query = new Query();

    /**
     * 校验配置，非法时抛出 {@link IllegalArgumentException}。
     */
    public void validate() {
        if (tableName == null || !TABLE_NAME_PATTERN.matcher(tableName).matches()) {
            throw new IllegalArgumentException(
                "itsuka.log.tableName 只能包含字母、数字、下划线，且长度为 1~64: " + tableName);
        }
        if (contentMaxLength <= 0) {
            throw new IllegalArgumentException("itsuka.log.contentMaxLength 必须大于 0: " + contentMaxLength);
        }
        if (detailMaxLength <= 0) {
            throw new IllegalArgumentException("itsuka.log.detailMaxLength 必须大于 0: " + detailMaxLength);
        }
        if (errorMaxLength <= 0) {
            throw new IllegalArgumentException("itsuka.log.errorMaxLength 必须大于 0: " + errorMaxLength);
        }
        if (async == null) {
            throw new IllegalArgumentException("itsuka.log.async 不能为空");
        }
        if (async.getCorePoolSize() < 1) {
            throw new IllegalArgumentException(
                "itsuka.log.async.corePoolSize 必须大于等于 1: " + async.getCorePoolSize());
        }
        if (async.getMaxPoolSize() < async.getCorePoolSize()) {
            throw new IllegalArgumentException(
                "itsuka.log.async.maxPoolSize 必须大于等于 corePoolSize: maxPoolSize="
                    + async.getMaxPoolSize() + ", corePoolSize=" + async.getCorePoolSize());
        }
        if (async.getQueueCapacity() < 0) {
            throw new IllegalArgumentException(
                "itsuka.log.async.queueCapacity 必须大于等于 0: " + async.getQueueCapacity());
        }
        if (request == null) {
            throw new IllegalArgumentException("itsuka.log.request 不能为空");
        }
        if (request.getParamsMaxLength() <= 0) {
            throw new IllegalArgumentException(
                "itsuka.log.request.paramsMaxLength 必须大于 0: " + request.getParamsMaxLength());
        }
        if (query == null) {
            throw new IllegalArgumentException("itsuka.log.query 不能为空");
        }
        if (query.getDefaultPageSize() < 1) {
            throw new IllegalArgumentException(
                "itsuka.log.query.defaultPageSize 必须大于等于 1: " + query.getDefaultPageSize());
        }
        if (query.getMaxPageSize() < query.getDefaultPageSize()) {
            throw new IllegalArgumentException(
                "itsuka.log.query.maxPageSize 必须大于等于 defaultPageSize: maxPageSize="
                    + query.getMaxPageSize() + ", defaultPageSize=" + query.getDefaultPageSize());
        }
    }

    /**
     * 存储方式
     */
    public enum Storage {
        /**
         * 写入数据库
         */
        JDBC,
        /**
         * 写入独立 logger，不落库
         */
        SLF4J
    }

    /**
     * 线程池拒绝策略
     */
    public enum RejectionPolicy {
        /**
         * 由调用线程执行
         */
        CALLER_RUNS,
        /**
         * 抛出异常
         */
        ABORT,
        /**
         * 丢弃任务
         */
        DISCARD,
        /**
         * 丢弃队列中最旧的任务
         */
        DISCARD_OLDEST
    }

    /**
     * 异步写入配置
     */
    @Data
    public static class Async {

        /**
         * 是否异步写入
         */
        private boolean enabled = false;

        /**
         * 核心线程数
         */
        private int corePoolSize = 1;

        /**
         * 最大线程数
         */
        private int maxPoolSize = 4;

        /**
         * 队列容量
         */
        private int queueCapacity = 1024;

        /**
         * 空闲线程存活时间
         */
        private Duration keepAlive = Duration.ofSeconds(60);

        /**
         * 线程名前缀
         */
        private String threadNamePrefix = "itsuka-log-";

        /**
         * 拒绝策略
         */
        private RejectionPolicy rejectionPolicy = RejectionPolicy.CALLER_RUNS;

        /**
         * 关闭时等待任务完成的时间
         */
        private Duration awaitTermination = Duration.ofSeconds(5);
    }

    /**
     * 操作人配置
     */
    @Data
    public static class Operator {

        /**
         * 取不到登录人时的操作人 ID
         */
        private String defaultId = "system";

        /**
         * 取不到登录人时的操作人名称
         */
        private String defaultName = "系统";
    }

    /**
     * 请求信息配置
     */
    @Data
    public static class Request {

        /**
         * 是否记录 IP/UA
         */
        private boolean enabled = true;

        /**
         * 是否信任 X-Forwarded-For / X-Real-IP
         */
        private boolean trustForwardedHeaders = false;

        /**
         * 是否记录参数快照
         */
        private boolean paramsEnabled = false;

        /**
         * 参数快照截断长度
         */
        private int paramsMaxLength = 4000;
    }

    /**
     * 事务配置
     */
    @Data
    public static class Transaction {

        /**
         * JDBC 写入是否使用独立事务
         */
        private boolean requiresNew = true;
    }

    /**
     * 查询配置
     */
    @Data
    public static class Query {

        /**
         * 是否注册默认查询服务
         */
        private boolean enabled = true;

        /**
         * 默认每页大小
         */
        private int defaultPageSize = 20;

        /**
         * 最大每页大小
         */
        private int maxPageSize = 200;
    }
}
