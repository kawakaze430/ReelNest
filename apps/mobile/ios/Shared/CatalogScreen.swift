import Foundation
import SwiftUI

struct TitleItem: Decodable, Identifiable {
    let id: UUID
    let name: String
    let synopsis: String?
    let genre: String?
    let ageRating: String?
}
private struct CatalogResponse: Decodable {
    let content: [TitleItem]
}

@MainActor final class CatalogViewModel: ObservableObject {
    @Published var items: [TitleItem] = []
    @Published var message = "Checking the public catalog..."

    func load() async {
        guard let raw = Bundle.main.object(forInfoDictionaryKey: "ReelNestAPIOrigin") as? String,
              raw != "https://api.example.org",
              let url = URL(string: raw + "/api/v1/catalog?page=0&size=24"),
              url.scheme == "https", url.user == nil, url.password == nil,
              let host = url.host, !host.isEmpty else {
            message = "Configure a reviewed HTTPS API origin. Playback is disabled."
            return
        }
        do {
            var request=URLRequest(url:url)
            request.httpMethod="GET"
            request.cachePolicy = .reloadIgnoringLocalCacheData
            request.timeoutInterval=10
            let (data,response)=try await URLSession.shared.data(for:request)
            guard (response as? HTTPURLResponse)?.statusCode==200,
                  data.count < 2_000_000 else {
                message="Catalog response unavailable"
                return
            }
            let result=try JSONDecoder().decode(CatalogResponse.self,from:data)
            items=Array(result.content.prefix(24))
            message=items.isEmpty ? "No published titles" : "Browse titles. Protected playback is not implemented."
        } catch {
            message="Cannot load the public catalog. Check the approved API and network."
        }
    }
}

struct CatalogScreen: View {
    @StateObject private var model=CatalogViewModel()
    var body: some View {
        NavigationStack {
            List {
                Text(model.message).accessibilityLabel(model.message)
                ForEach(model.items) { item in
                    NavigationLink {
                        VStack(alignment:.leading,spacing:16){
                            Text(item.name).font(.title)
                            Text(item.synopsis ?? "")
                            Text("Streaming is disabled pending signed-in playback, rights checks and FairPlay certification.")
                                .font(.footnote)
                        }.padding()
                    } label: {
                        VStack(alignment:.leading){
                            Text(item.name).font(.headline)
                            Text(item.genre ?? "")
                        }
                    }
                }
            }
            .navigationTitle("ReelNest Catalog Preview")
            .task { await model.load() }
        }
    }
}
