import Foundation

/** Read-only custom-scheme catalog link. This is never an OAuth redirect URI. */
enum SafeCatalogLink {
    static func titleId(from url: URL) -> UUID? {
        guard url.scheme == "reelnest-preview", url.host == "catalog",
              url.user == nil, url.password == nil, url.port == nil,
              url.query == nil, url.fragment == nil else { return nil }
        let parts=url.pathComponents
        guard parts.count == 3, parts[1] == "title",
              let uuid=UUID(uuidString:parts[2]),
              uuid.uuidString.lowercased() == parts[2].lowercased() else { return nil }
        return uuid
    }
}
