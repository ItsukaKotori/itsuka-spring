package org.itsuka.core.domian;

import java.util.Collections;

public class AnonymousLoginUser extends LoginUser {
    public AnonymousLoginUser() {
        super(-1L, "anonymous", "匿名用户",
                null, true, false, Collections.emptyList());
    }
}
