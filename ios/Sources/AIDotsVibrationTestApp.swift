import SwiftUI
import UIKit

@main
struct AIDotsVibrationTestApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

struct ContentView: View {
    @State private var messages: [ChatMessage] = [
        ChatMessage(text: "Send any message to read an offline story. Haptics run as the story appears. Use the test button to check haptics immediately.", isUser: false)
    ]
    @State private var input = ""
    @State private var status = "Ready"
    @State private var isGenerating = false
    @State private var generationTask: Task<Void, Never>?
    @State private var impact = UIImpactFeedbackGenerator(style: .light)

    private let story = """
    One quiet evening, a young traveler discovered a small lantern beside an old stone bridge. Inside the lantern was a note: Even the smallest light can guide someone through the darkest night. The traveler carried the lantern into the village and helped an elderly neighbor find the way home. The next morning, the villagers placed lanterns along every street, and soon the whole village glowed. The traveler understood the message: a small act of kindness can inspire many others. From that day on, the village remembered that helping one person can brighten an entire world.
    """

    var body: some View {
        VStack(spacing: 10) {
            Text("AI Dots")
                .font(.title2.bold())
                .foregroundStyle(.primary)
                .padding(.top, 8)

            Text(status)
                .font(.footnote)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)

            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 12) {
                        ForEach(messages) { message in
                            HStack {
                                if message.isUser { Spacer(minLength: 36) }
                                Text(message.text.isEmpty ? " " : message.text)
                                    .font(.body)
                                    .foregroundStyle(message.isUser ? Color.white : Color.primary)
                                    .padding(12)
                                    .background(message.isUser ? Color.blue : Color(.secondarySystemBackground))
                                    .clipShape(RoundedRectangle(cornerRadius: 16))
                                if !message.isUser { Spacer(minLength: 36) }
                            }
                            .id(message.id)
                        }
                    }
                    .padding(.vertical, 8)
                }
                .onChange(of: messages.last?.text) { _ in
                    if let lastID = messages.last?.id {
                        withAnimation(.easeOut(duration: 0.12)) {
                            proxy.scrollTo(lastID, anchor: .bottom)
                        }
                    }
                }
            }

            Button(action: testHaptics) {
                Text("TEST VIBRATION NOW")
                    .font(.body.weight(.semibold))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
            }
            .buttonStyle(.borderedProminent)

            HStack(spacing: 8) {
                TextField("Type a message...", text: $input)
                    .textFieldStyle(.roundedBorder)
                    .submitLabel(.send)
                    .onSubmit(sendMessage)
                    .disabled(isGenerating)

                Button("Send", action: sendMessage)
                    .buttonStyle(.borderedProminent)
                    .disabled(isGenerating || input.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
            .padding(.bottom, 4)
        }
        .padding(.horizontal, 16)
        .background(Color(.systemBackground))
        .onAppear { impact.prepare() }
        .onDisappear {
            generationTask?.cancel()
            isGenerating = false
        }
    }

    private func testHaptics() {
        impact.impactOccurred(intensity: 0.9)
        impact.prepare()
        status = "Haptic test requested"
    }

    private func sendMessage() {
        let message = input.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !message.isEmpty, !isGenerating else { return }

        messages.append(ChatMessage(text: message, isUser: true))
        input = ""
        messages.append(ChatMessage(text: "", isUser: false))
        let responseID = messages[messages.count - 1].id
        startStory(for: responseID)
    }

    private func startStory(for responseID: UUID) {
        isGenerating = true
        status = "Generating story — haptics active"
        impact.prepare()
        let characters = Array(story)
        generationTask = Task { @MainActor in
            for character in characters {
                if Task.isCancelled { break }

                if let index = messages.firstIndex(where: { $0.id == responseID }) {
                    messages[index].text.append(character)
                }

                if !character.isWhitespace {
                    impact.impactOccurred(intensity: character.isPunctuation ? 0.8 : 0.65)
                    impact.prepare()
                }

                let delay: UInt64 = (character == "." || character == "," || character == ":") ? 220_000_000 : 100_000_000
                try? await Task.sleep(nanoseconds: delay)
            }

            if !Task.isCancelled {
                status = "Story complete"
            } else {
                status = "Stopped"
            }
            isGenerating = false
            generationTask = nil
        }
    }
}

private struct ChatMessage: Identifiable {
    let id = UUID()
    var text: String
    let isUser: Bool
}
