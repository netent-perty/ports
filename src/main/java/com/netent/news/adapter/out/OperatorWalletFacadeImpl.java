package com.netent.news.adapter.out;

import com.netent.news.domain.WalletId;
import com.netent.news.application.port.out.OperatorGetBalance;
import com.netent.news.application.port.out.OperatorWalletFacade;
import org.springframework.stereotype.Component;

@Component
public class OperatorWalletFacadeImpl implements OperatorWalletFacade {
    @Override
    public OperatorGetBalance getBalance(WalletId wallet) {
        return new OperatorGetBalance();
    }
}
