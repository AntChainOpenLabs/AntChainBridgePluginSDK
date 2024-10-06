package org.example.plugintestrunner.service;

import org.example.plugintestrunner.chainmanager.IChainManager;
import org.example.plugintestrunner.chainmanager.IChainManagerFactory;
import org.example.plugintestrunner.config.ChainProduct;
import org.example.plugintestrunner.exception.ChainManagerException;
import org.example.plugintestrunner.exception.ChainManagerException.*;
import org.example.plugintestrunner.testcase.TestCase;
import org.example.plugintestrunner.util.LogLevel;
import org.example.plugintestrunner.util.PTRLogger;
import org.example.plugintestrunner.util.ShellScriptRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 启动/关闭测试链
 */

public class ChainManagerService extends AbstractService {

    // 用于执行 shell 脚本
    private final ShellScriptRunner shellScriptRunner;

    private final Map<String, IChainManager> tmpChainManagers;
    private final Map<String, IChainManager> localChainManagers;

    private static final String STARTUP_SUFFIX = "_startup.sh";
    private static final String SHUTDOWN_SUFFIX = "_shutdown.sh";

    public ChainManagerService(PTRLogger logger, ShellScriptRunner shellScriptRunner) {
        super(logger);
        this.shellScriptRunner = shellScriptRunner;
        this.tmpChainManagers = new java.util.HashMap<>();
        this.localChainManagers = new java.util.HashMap<>();
    }

    /**
     * 根据 {@code testCase} 的配置信息，可以分为两种情况：
     * <ul>
     *   <li>
     *     1. 测试用例中的 {@code chainConf} 为空，或配置信息不完整时，将启动一个临时的测试链，具体分为两种情况：
     *     <ul>
     *       <li>1.1. 该类型的链已经存在 {@code tmpChainManagers}，不需要重新创建</li>
     *       <li>1.2. 该类型的链不存在 {@code tmpChainManagers}，则直接创建</li>
     *     </ul>
     *   </li>
     *   <li>
     *     2. 测试用例中的 {@code chainConf} 不为空，并且信息完整，直接使用该配置信息，又具体分为两种情况：
     *     <ul>
     *       <li>2.1. 根据 {@code chainConf} 创建 {@code chainManager}，存入 {@code localChainManagers}，无需重新创建</li>
     *     </ul>
     *   </li>
     * </ul>
     */
    @Override
    public void run(TestCase testCase) {
        // 设置 MDC
        logger.putMDC(testCase.getName());

        // 测试用例中的 chainConf 为空，或配置信息不完整时，将启动一个临时的测试链
        if (!testCase.isChainConfValid()) {
            try {
                initializeTemporalChain(testCase);
                logger.plog(LogLevel.INFO, "Successfully initialized a temporary chain");
            } catch (ChainManagerException e) {
                logger.plog(LogLevel.ERROR, "Failed to initialize a temporary chain" + e.getMessage());
                if (e.getCause() != null) {
                    logger.rlog(LogLevel.ERROR, e.getCause().getMessage());
                }
            } catch (InterruptedException e) {
                logger.plog(LogLevel.ERROR, "Failed to sleep");
            }
        }
        else {
            try {
                IChainManager chainManager = testCase.getChainConf().toChainManager();
                localChainManagers.put(testCase.getProduct(), chainManager);
                logger.plog(LogLevel.INFO, "Successfully initialized local chain");
            } catch (Exception e) {
                logger.plog(LogLevel.ERROR, "Failed to initialize local chain: " + e.getMessage());
                if (e.getCause() != null) {
                    logger.rlog(LogLevel.ERROR, e.getCause().getMessage());
                }
            }
        }

        // 清除 MDC
        logger.clearMDC();
    }

    @Override
    public void close() {
        for (String product : tmpChainManagers.keySet()) {
            // 关闭 chainManager 连接
            tmpChainManagers.get(product).close();
            // 执行关闭脚本
            try {
                shutdown(product);
            } catch (ChainManagerException | IOException | InterruptedException e) {
                logger.plog(LogLevel.ERROR, "Failed to shutdown chain: " + product);
                if (e.getCause() != null) {
                    logger.rlog(LogLevel.ERROR, e.getCause().getMessage());
                }
            }
        }
    }


    /**
     * <p>
     * Step 1: 判断 {@code chainProduct} 是否合法
     * </p>
     * <p>
     * Step 2: 判断是否已经存在该类型的链
     * </p>
     * <p>
     * Step 3: 若不存在，则启动测试链
     * </p>
     */
    private void initializeTemporalChain(TestCase testCase) throws ChainManagerException, InterruptedException {
        logger.plog(LogLevel.INFO, "No chain configuration found in test case, start a temporary chain");
        String product = testCase.getProduct();
        if (ChainProduct.isInvalid(product)) {
            throw new InvalidProductException("Invalid product: " + product);
        } else {
            // 判断是否已经存在该类型的链
            if (tmpChainManagers.get(product) != null) {
                logger.plog(LogLevel.INFO, "Chain for " + product + " already exists, no need to start a new one");
            } else {
                // 启动测试链
                try {
                    startup(product);
                } catch (Exception e) {
                    throw new ChainManagerException("Failed to start temporary chain: " + product, e);
                }
            }
        }
    }

    /**
     * <p>
     * step1. 判断 {@code chainProduct} 是否合法
     * </p>
     * <p>
     * step2. 判断是否已经存在该类型的链
     * </p>
     * <p>
     * step3. 若不存在，运行脚本启动测试链
     * </p>
     * <p>
     * step4. 创建 {@code ChainConfig}
     * </p>
     * <p>
     * step5. 创建 {@code ChainManager}
     * </p>
     */
    public void startup(String product) throws ChainManagerException, InterruptedException, IOException {
        if (ChainProduct.isInvalid(product)) {
            throw new InvalidProductException("Invalid product: " + product);
        } else if (tmpChainManagers.get(product) != null) {
            logger.plog(LogLevel.INFO, "Chain for " + product + " already exists");
        } else {
            String scriptName = product + STARTUP_SUFFIX;
            if (!shellScriptRunner.scriptExists(product, scriptName)) {
                throw new ScriptNotFoundException("Script not found: " + scriptName);
            }
            shellScriptRunner.runScript(product, scriptName);
        }
        logger.plog(LogLevel.INFO, "Successfully start chain: " + product);
        // 休眠 5 秒，等待链启动完成
        logger.plog(LogLevel.INFO, "Sleep 5 seconds to wait for chain startup");
        Thread.sleep(5000);

        // 创建 chainManager
        IChainManager chainManager;
        try {
            chainManager = IChainManagerFactory.createIChainManager(product);
        } catch (Exception e) {
            throw new ChainManagerException("Failed to create chainManager for chain: " + product, e);
        }

        // 添加到 tmpChainManagers 中
        tmpChainManagers.put(product, chainManager);
        logger.plog(LogLevel.INFO, "Successfully initialized chainManager for chain: " + product);
    }

    public void shutdown(String product) throws ChainManagerException, IOException, InterruptedException {
        if (ChainProduct.isInvalid(product)) {
            throw new InvalidProductException("Invalid product: " + product);
        } else if (tmpChainManagers.get(product) == null) {
            throw new ChainManagerNotInitializedException("ChainManager not initialized for chain: " + product);
        } else {
            // 先关闭 chainManager 连接
            tmpChainManagers.get(product).close();
            // 执行关闭脚本
            String scriptName = product + SHUTDOWN_SUFFIX;
            if (!shellScriptRunner.scriptExists(product, scriptName)) {
                throw new ScriptNotFoundException("Script not found: " + scriptName);
            }
            shellScriptRunner.runScript(product, scriptName);
            // 从 chainManagers 中移除
            tmpChainManagers.remove(product);
        }
        logger.plog(LogLevel.INFO, "Successfully shutdown chain: " + product);
    }

    //    显示正在运行的 chainManager
    public void showChainManagers() {
        List<String> runningChainManagers = new ArrayList<>(tmpChainManagers.keySet());
        logger.rlog(LogLevel.INFO, runningChainManagers.toString());
    }

    public IChainManager getChainManager(String product) throws ChainManagerNotFoundExpcetion{
        IChainManager iChainManager = tmpChainManagers.get(product);
        if (iChainManager == null) {
            iChainManager = localChainManagers.get(product);
            if (iChainManager == null) {
                throw new ChainManagerNotFoundExpcetion("ChainManager not fonud!");
            } else {
                return iChainManager;
            }
        } else {
            return iChainManager;
        }
    }
}