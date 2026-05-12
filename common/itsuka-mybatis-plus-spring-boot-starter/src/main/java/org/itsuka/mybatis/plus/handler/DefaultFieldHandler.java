package org.itsuka.mybatis.plus.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.reflection.MetaObject;
import org.itsuka.core.domain.LoginUser;
import org.itsuka.web.utils.WebHelper;

/**
 * 字段填充
 *
 * @author itsuka
 */
@RequiredArgsConstructor
public class DefaultFieldHandler implements MetaObjectHandler {

    private final WebHelper helper;

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(
            metaObject,
            "createTime",
            LocalDateTime.class,
            now
        );
        this.strictInsertFill(
            metaObject,
            "updateTime",
            LocalDateTime.class,
            now
        );
        Optional<LoginUser> optional = helper.getLoginUser();
        if (optional.isPresent()) {
            LoginUser loginUser = optional.get();
            this.strictInsertFill(
                metaObject,
                "createBy",
                String.class,
                loginUser.getUsername()
            );
            this.strictInsertFill(
                metaObject,
                "updateBy",
                String.class,
                loginUser.getUsername()
            );
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictUpdateFill(
            metaObject,
            "updateTime",
            LocalDateTime.class,
            now
        );
        Optional<LoginUser> optional = helper.getLoginUser();
        if (optional.isPresent()) {
            LoginUser loginUser = optional.get();
            this.strictUpdateFill(
                metaObject,
                "updateBy",
                String.class,
                loginUser.getUsername()
            );
        }
    }
}
