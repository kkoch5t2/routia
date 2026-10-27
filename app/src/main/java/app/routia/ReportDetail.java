package app.routia;

/** Prose depth and matching limits; character ranges are guidance, never a quota. */
public enum ReportDetail {
    BRIEF("brief","簡潔","要点と、優先する改善策を短く","250〜450",600,1,
        60,18,100,20,100,45,100,70,20,70,55,
        "要点と最優先の改善策を各1件に絞る。理由と試したあとの確認は省略せず、短く具体的に書く。"),
    STANDARD("standard","標準","傾向・改善策・根拠をひと通り","500〜800",1000,2,
        70,18,90,20,90,45,85,60,20,70,55,
        "判断に役立つ傾向、本人に合う改善策、その理由と研究上の根拠をひと通り説明する。"),
    DETAILED("detailed","詳細","提案の理由・試し方まで詳しく","1,400〜2,200",2800,2,
        100,24,320,28,280,100,300,220,28,220,120,
        "記録の具体的な比較と複数の解釈を検討し、提案を選ぶ本人固有の理由、次回の手順、試行後の確かめ方、研究が支える範囲を掘り下げる。実記録が少なければ短くてよい。提案は優先度の高い1つを基本とし、必要な場合だけ2つ。");

    public final String id,label,description,range;
    public final int total,cards,headline,insightTitle,insightDetail,actionTitle,why,cue,step,effect,lensName,lensDetail,limit;
    private final String guidance;
    ReportDetail(String id,String label,String description,String range,int total,int cards,
        int headline,int insightTitle,int insightDetail,int actionTitle,int why,int cue,int step,int effect,int lensName,int lensDetail,int limit,String guidance){
        this.id=id;this.label=label;this.description=description;this.range=range;this.total=total;this.cards=cards;
        this.headline=headline;this.insightTitle=insightTitle;this.insightDetail=insightDetail;this.actionTitle=actionTitle;
        this.why=why;this.cue=cue;this.step=step;this.effect=effect;this.lensName=lensName;this.lensDetail=lensDetail;this.limit=limit;this.guidance=guidance;
    }
    public static ReportDetail fromId(String id){
        for(ReportDetail d:values())if(d.id.equals(id))return d;
        throw new IllegalArgumentException("レポートの詳しさを確認してください");
    }
    public String instructions(){
        return "\n【レポートの詳しさ："+label+"】\n日本語の本文合計は"+range+"字程度、最大"+total+"字。"
            +guidance+" 各配列は1〜"+cards+"件。文字数は目安であり、根拠のない補完・同じ説明の繰り返し・水増しをしない。必要な内容を優先し、目安の下限に届かなくてもよい。\n";
    }
}
