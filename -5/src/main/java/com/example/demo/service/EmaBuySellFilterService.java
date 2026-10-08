
package com.example.demo.service;

import com.example.demo.model.EmaTrade;
import com.example.demo.repository.EmaTradeRepository;
import com.example.demo.service.LiveEmaCalculatorService.TrendResult;
import com.example.demo.webSocket.EmaBuySellHandler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class EmaBuySellFilterService {

	private final EmaBuySellHandler emaBuySellHandler;

	private final EmaTradeRepository emaTradeRepository;

	public EmaBuySellFilterService(EmaBuySellHandler emaBuySellHandler, EmaTradeRepository emaTradeRepository) {

		this.emaBuySellHandler = emaBuySellHandler;
		this.emaTradeRepository = emaTradeRepository;

	}

	List<EmaTrade> emaTrades = new ArrayList<>();

	public void processAllStocks(TrendResult trade) {

		EmaTrade emaTrade = new EmaTrade();

		if (trade.getBearishStrength() > 90 && trade.getInstrumentKey().equals("NSE_EQ|INE090A01021")) {

			emaTrade.setSellPrice(trade.getCurrentPrice());
			emaTrade.setInstrumentKey(trade.getInstrumentKey());
			emaTrade.setStatus("SELL");
			emaTrade.setTrend(String.valueOf(trade.getTrend()));

			emaTrade.setSellTime(trade.getTimestamp());

			System.out.println(trade.getBearishStrength()+ " / "+trade.getInstrumentKey()+" / "+trade.getTimestamp()+" / "+trade.getCurrentPrice()+" / "+trade.getEma20()
			+" / "+trade.getEma50()+" / "+trade.getEma200());

			//emaTrades.add(emaTrade);
		}

		if (trade.getBearishStrength() < 90) {

			Iterator<EmaTrade> iterator = emaTrades.iterator();

			while (iterator.hasNext()) {

				EmaTrade e1 = iterator.next();

				if (e1.getInstrumentKey().equals(trade.getInstrumentKey())) {

					e1.setBuyPrice(trade.getCurrentPrice());
					
					e1.setBuyTime(trade.getTimestamp());

					
					
			//emaTradeRepository.save(e1);

					iterator.remove();

					break;
				}
			}
		}
	}

	public List<EmaTrade> getAllTrades() {
		return emaTradeRepository.findAll();
	}

}