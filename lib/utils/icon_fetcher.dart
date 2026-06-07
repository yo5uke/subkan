class IconFetcher {
  /// サービス名のキーワード -> 公式ドメインのマッピング。
  ///
  /// 以前はWikimediaのサムネイル画像URLを直接埋め込んでいたが、
  /// Wikimediaは適切なUser-Agentの無いリクエストに403を返すうえ、
  /// サムネイルのパス（ハッシュ・ファイル名・`128px-`プレフィックス）が
  /// 少しでも変わると404になり、アイコンが安定して表示されなかった。
  ///
  /// 代わりにキーワードから公式ドメインを引き、CDN配信・キャッシュされる
  /// GoogleのfaviconサービスへURLを組み立てることで、確実にロゴを表示する。
  static const Map<String, String> _domainByKeyword = {
    'claude': 'claude.ai',
    'anthropic': 'claude.ai',
    'chatgpt': 'openai.com',
    'openai': 'openai.com',
    'gemini': 'gemini.google.com',
    'youtube': 'youtube.com',
    'amazon': 'amazon.co.jp',
    'prime': 'amazon.co.jp',
    'netflix': 'netflix.com',
    'spotify': 'spotify.com',
    'notion': 'notion.so',
    'hulu': 'hulu.com',
    'github': 'github.com',
    'disney': 'disneyplus.com',
    'microsoft': 'microsoft.com',
    'office': 'microsoft.com',
    'm365': 'microsoft.com',
    'adobe': 'adobe.com',
    'photoshop': 'adobe.com',
    'illustrator': 'adobe.com',
    'nintendo': 'nintendo.com',
    'switch': 'nintendo.com',
    'playstation': 'playstation.com',
    'psplus': 'playstation.com',
    'psn': 'playstation.com',
    'apple': 'apple.com',
    'icloud': 'apple.com',
    'unext': 'unext.jp',
    'u-next': 'unext.jp',
    'duolingo': 'duolingo.com',
    'canva': 'canva.com',
    'slack': 'slack.com',
    'dropbox': 'dropbox.com',
    'figma': 'figma.com',
    'zoom': 'zoom.us',
    'dazn': 'dazn.com',
    // Google系は他の派生サービス（gemini/youtube等）の後に置き、誤マッチを防ぐ
    'google': 'google.com',
  };

  /// サービス名から表示用のアイコン画像URLを生成する。
  ///
  /// ロゴを確実に特定できる場合のみURLを返し、特定できない場合は空文字を返す。
  /// 空文字のときは呼び出し側がM3スタイルのフォールバックタイルを表示するため、
  /// 「ドメインが分からない＝Googleが返す汎用の地球儀アイコン」を表示せずに済む。
  ///
  /// URLを返すのは次のいずれかの場合:
  ///   1. 既知サービスのキーワードに一致（公式ドメインを使用）
  ///   2. ユーザーがドメイン形式（"example.com" 等）で入力した
  static String getIconUrl(String serviceName) {
    final nameLower = serviceName.trim().toLowerCase();
    if (nameLower.isEmpty) return '';

    String? domain;
    for (final entry in _domainByKeyword.entries) {
      if (nameLower.contains(entry.key)) {
        domain = entry.value;
        break;
      }
    }

    // キーワード未一致でも、ドメイン形式の入力ならそのドメインを使う。
    if (domain == null) {
      final cleaned = nameLower.replaceAll(RegExp(r'\s+'), '');
      if (cleaned.contains('.')) domain = cleaned;
    }

    // ロゴを特定できない場合は空文字を返し、フォールバックタイルに委ねる。
    if (domain == null) return '';

    // Googleのfaviconサービス（実体はt3.gstaticへリダイレクトされCORSヘッダを持たない）を、
    // CORS対応の画像プロキシ wsrv.nl 経由で取得する。
    //
    // Flutter Web（CanvasKit）の CachedNetworkImage は画像を「バイト取得」するため、
    // 取得元が Access-Control-Allow-Origin を返さないとCORSで失敗し、アイコンが表示されない。
    // wsrv.nl は `Access-Control-Allow-Origin: *` を返すうえリダイレクトも追従するため、
    // Web/モバイル双方で確実にアイコンを表示できる。
    final faviconUrl = 'https://www.google.com/s2/favicons?domain=$domain&sz=128';
    return 'https://wsrv.nl/?url=${Uri.encodeComponent(faviconUrl)}&w=128&h=128&output=png';
  }
}
