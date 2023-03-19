package org.itsuka.mybatis.plus.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.reflection.MetaObject;
import org.itsuka.core.domian.LoginUser;
import org.itsuka.web.utils.WebHelper;

import java.time.LocalDateTime;
import java.util.Optional;

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
        this.strictInsertFill(metaObject, "create_time", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "update_time", LocalDateTime.class, now);
        Optional<LoginUser> optional = helper.getLoginUser();
        if (optional.isPresent()) {
            LoginUser loginUser = optional.get();
            this.strictInsertFill(metaObject, "create_by", String.class, loginUser.getUsername());
            this.strictInsertFill(metaObject, "update_by", String.class, loginUser.getUsername());
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "update_time", LocalDateTime.class, now);
        Optional<LoginUser> optional = helper.getLoginUser();
        if (optional.isPresent()) {
            LoginUser loginUser = optional.get();
            this.strictInsertFill(metaObject, "update_by", String.class, loginUser.getUsername());
        }
    }
}
