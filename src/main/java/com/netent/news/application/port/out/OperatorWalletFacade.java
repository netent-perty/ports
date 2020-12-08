package com.netent.news.application.port.out;

import com.netent.news.domain.WalletId;
import org.springframework.stereotype.Service;

@Service
public interface OperatorWalletFacade {
    OperatorGetBalance getBalance(WalletId wallet);
}
