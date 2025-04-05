import java.util.Random;

public class TechnicalAgent extends Agent{

    // 参照する過去の市場価格の時刻
    private static int PAST_TIME = 100000;
    // 注文価格の最大値
    private static int PRICE_MAX = 100000;
    
    // 所持しているポジション
	// 買いポジション：+1
	// 売りポジション：-1
	private int position;
    // エージェントの買いと売りの判断
	private Flag.Trade tradeFlag;
    // エージェントの注文数
    private int ORDER_NUM = 1;

    // パラメータクラス
    Parameter param;
    // 乱数
    Random rand;

    public TechnicalAgent(Parameter param, int agentId) {
        super(); // 親クラスのコンストラクタを呼び出す
        
        this.param = param;
        rand = new Random(param.getSeedForAgent(agentId + 20000)); // 重複を避ける
        
        // ポジション
        this.position = 0;
        // 初期状態では注文なし
        this.tradeFlag = Flag.Trade.NONE;
    }

    // 注文の発注
	public Flag.Trade order(Market market, int time, int i) {
        
        // 以前の市場価格
        double pastmarketPrice = market.getPreMarketPrice(PAST_TIME, time);
        // 最良買い気配値
		double bestBid = market.getBestBid();
        // 最良売り気配値
        double bestAsk = market.getBestAsk();

        // 株式を所有していない場合
        if(this.position == 0){
            // 以前の市場価格と最良気配値を比較
            // 下降トレンドがある場合は売り
            if(pastmarketPrice > bestBid){
                this.tradeFlag = Flag.Trade.SELL;
            } 
            // 上昇トレンドがある場合は買い
            else if(pastmarketPrice < bestAsk){
                this.tradeFlag = Flag.Trade.BUY;
            } 
            // 同じ場合は注文なし
            else {
                this.tradeFlag = Flag.Trade.NONE;
            }
        } // 株式を所有している場合
        else if(this.position == 1){
            // 以前の市場価格と最良気配値を比較
            // 下降トレンドがある場合は売り
            if(pastmarketPrice > bestBid){
                this.tradeFlag = Flag.Trade.SELL;
            } 
            // 上昇トレンドがある場合は注文なし
            else if(pastmarketPrice < bestAsk){
                this.tradeFlag = Flag.Trade.NONE;
            } 
            // 同じ場合は注文なし
            else {
                this.tradeFlag = Flag.Trade.NONE;
            }
        } // 株式を空売りしている場合
        else if(this.position == -1){
            // 以前の市場価格と最良気配値を比較
            // 下降トレンドがある場合は注文なし
            if(pastmarketPrice > bestBid){
                this.tradeFlag = Flag.Trade.NONE;
            } 
            // 上昇トレンドがある場合は買い
            else if(pastmarketPrice < bestAsk){
                this.tradeFlag = Flag.Trade.BUY;
            } 
            // 同じ場合は注文なし
            else {
                this.tradeFlag = Flag.Trade.NONE;
            }
        }

        // 発注処理
		if(this.tradeFlag != Flag.Trade.NONE) {
			// 板形成期間中は注文しない
			if(time <= this.param.getT_bookMake()) {
				this.tradeFlag = Flag.Trade.NONE;
			}
			if(tradeFlag == Flag.Trade.NONE) {
				return this.tradeFlag; 
			}

			// 市場に注文を出す
			double orderPrice;
			// 買い注文の場合
            if(this.tradeFlag == Flag.Trade.BUY) {
                // 1回目の注文 - 現在の最良売り気配値で成行注文
                orderPrice = market.getBestAsk(); // 最新の最良売り気配値を取得
                Order order1 = new Order(this, time, orderPrice, ORDER_NUM, this.tradeFlag);
			    market.addOrder(order1);
                
                // 株式を所有していない場合は1回だけの注文に制限
                if(this.position != 0) {
                    // 2回目の注文 - 再度最新の最良売り気配値を取得して成行注文
                    orderPrice = market.getBestAsk();
                    Order order2 = new Order(this, time, orderPrice, ORDER_NUM, this.tradeFlag);
			        market.addOrder(order2);
                }
            } 
            // 売り注文の場合
            else if(this.tradeFlag == Flag.Trade.SELL) {
                // 1回目の注文 - 現在の最良買い気配値で成行注文
                orderPrice = market.getBestBid(); // 最新の最良買い気配値を取得
                Order order1 = new Order(this, time, orderPrice, ORDER_NUM, this.tradeFlag);
			    market.addOrder(order1);
                
                // 株式を所有していない場合は1回だけの注文に制限
                if(this.position != 0) {
                    // 2回目の注文 - 再度最新の最良買い気配値を取得して成行注文
                    orderPrice = market.getBestBid();
                    Order order2 = new Order(this, time, orderPrice, ORDER_NUM, this.tradeFlag);
			        market.addOrder(order2);
                }
            } 
            // その他の場合（通常はこの分岐には入らない）
            else {
                // 1回目の注文
                Order order1 = new Order(this, time, 10000, ORDER_NUM, this.tradeFlag);
			    market.addOrder(order1);
                
                // 株式を所有していない場合は1回だけの注文に制限
                if(this.position != 0) {
                    // 2回目の注文
                    Order order2 = new Order(this, time, 10000, ORDER_NUM, this.tradeFlag);
			        market.addOrder(order2);
                }
            }
		}
		return this.tradeFlag;
    }

    // 板形成期間の買いと売りの判断
	public Flag.Trade bookMakeOrderType(double orderPrice) {
		// 注文価格がファンダメンタル価格よりも高かった場合は売り注文
		if(orderPrice >= this.param.getP_fund()) {
			return Flag.Trade.SELL;
		}
		// 注文価格がファンダメンタル価格よりも低かった場合は買い注文
		else if(orderPrice < this.param.getP_fund()) {
			return Flag.Trade.BUY;
		}
		// ファンダメンタル価格と同じ場合
		else {
			return Flag.Trade.NONE;
		}
	}

    // ポジションの更新
	@Override
	public void updatePosition(Flag.Trade tradeFlag, int orderVol) {
		// 買い注文の場合：ポジションを増加
		if(tradeFlag == Flag.Trade.BUY) {
			position = position + orderVol;
		}
		// 売り注文の場合：ポジションを減少
		else if(tradeFlag == Flag.Trade.SELL) {
			position = position - orderVol;
		}
		// NONE の場合は何もしない
	}

    // getterメソッド群
    // 注文タイプを返す
	public Flag.Trade getTradeFlag() {
		return this.tradeFlag;
	}
	
	// 現在のポジションを返す
	public int getPosition() {
		return this.position;
	}
}