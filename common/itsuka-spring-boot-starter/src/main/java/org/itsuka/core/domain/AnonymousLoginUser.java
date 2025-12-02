package org.itsuka.core.domain;

import java.util.Collections;

public class AnonymousLoginUser extends LoginUser {
    public AnonymousLoginUser() {
        super(-1L, "anonymous", "匿名用户",
                null, null, null, true, false, Collections.emptyList());
    }
}
