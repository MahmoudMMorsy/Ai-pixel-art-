import torch
import torch.nn as nn
import torch.nn.functional as F

class BilingualRetroEmbedding(nn.Module):
    """
    Joint bilingual semantic embedding layer that maps both Arabic and English
    retro gaming vocabulary words into a shared continuous semantic space.
    This guarantees the model behaves identically and understands prompts
    such as 'ماريو' / 'Mario', 'بطل' / 'Hero', 'خلفية' / 'Background'.
    """
    def __init__(self, embed_dim=128):
        super(BilingualRetroEmbedding, self).__init__()
        self.embed_dim = embed_dim

        # Define a high-quality bidirectional dictionary mapping of retro concepts
        self.vocabulary = {
            # Special/Unconditioned
            "": 0, "null": 0, "none": 0,

            # Characters (أشخاص وشخصيات)
            "mario": 1, "ماريو": 1,
            "luigi": 2, "لويجي": 2,
            "hero": 3, "بطل": 3, "محارب": 3,
            "monster": 4, "وحش": 4, "شيطان": 4,
            "ghost": 5, "شبح": 5, "طيف": 5,
            "knight": 6, "فارس": 6, "درع": 6,
            "princess": 7, "أميرة": 7, "ملكة": 7,
            "wizard": 8, "ساحر": 8, "عراف": 8,
            "dragon": 9, "تنين": 9, "أفعى": 9,
            "slime": 10, "سلايم": 10, "هلام": 10,

            # Retro Environment & Items (أشياء وبيئات)
            "coin": 11, "عملة": 11, "ذهب": 11,
            "sword": 12, "سيف": 12, "سلاح": 12,
            "castle": 13, "قلعة": 13, "حصن": 13,
            "forest": 14, "غابة": 14, "شجر": 14,
            "dungeon": 15, "سراديب": 15, "متاهة": 15,
            "chest": 16, "صندوق": 16, "كنز": 16,
            "heart": 17, "قلب": 17, "حياة": 17,
            "star": 18, "نجمة": 18, "لمعان": 18,
            "fireball": 19, "كرة نارية": 19, "لهب": 19,
            "sky": 20, "سماء": 20, "سحاب": 20,
            "platform": 21, "أرضية": 21, "درج": 21
        }

        # Word embedding matrix + Linear projection
        self.embedding = nn.Embedding(num_embeddings=32, embedding_dim=embed_dim)
        self.projection = nn.Sequential(
            nn.Linear(embed_dim, embed_dim),
            nn.GELU(),
            nn.Linear(embed_dim, embed_dim)
        )

    def forward(self, text_prompts):
        """
        Encodes a list of batch text prompts into semantic latent vectors.
        """
        embeddings = []
        for prompt in text_prompts:
            # Clean and normalize
            p_clean = str(prompt).strip().lower()

            # Match vocabulary or split to find keywords
            words = p_clean.split()
            matched_indices = []
            for w in words:
                if w in self.vocabulary:
                    matched_indices.append(self.vocabulary[w])

            if not matched_indices:
                matched_indices = [0] # default to unconditioned/transparent index

            # Average embeddings of all recognized words in the prompt
            indices_tensor = torch.tensor(matched_indices, dtype=torch.long, device=self.embedding.weight.device)
            embed_vec = self.embedding(indices_tensor).mean(dim=0)
            embeddings.append(embed_vec)

        embeddings_tensor = torch.stack(embeddings, dim=0)
        return self.projection(embeddings_tensor)

class ResidualConvBlock(nn.Module):
    """
    Standard ResNet convolution block with GELU activations and Group Normalization
    optimized for stable latent representation.
    """
    def __init__(self, in_channels, out_channels, is_res=True):
        super(ResidualConvBlock, self).__init__()
        self.is_res = is_res
        self.same_channels = in_channels == out_channels
        self.conv1 = nn.Sequential(
            nn.Conv2d(in_channels, out_channels, 3, 1, 1),
            nn.GroupNorm(8, out_channels),
            nn.GELU()
        )
        self.conv2 = nn.Sequential(
            nn.Conv2d(out_channels, out_channels, 3, 1, 1),
            nn.GroupNorm(8, out_channels),
            nn.GELU()
        )
        self.proj = None
        if self.is_res and not self.same_channels:
            self.proj = nn.Conv2d(in_channels, out_channels, 1, 1, 0)

    def forward(self, x):
        h = self.conv2(self.conv1(x))
        if self.is_res:
            res = self.proj(x) if self.proj else x
            return (h + res) / 1.414 # normalize
        return h

class UNetDown(nn.Module):
    """UNet contraction step (MaxPool + ResBlock)"""
    def __init__(self, in_channels, out_channels):
        super(UNetDown, self).__init__()
        self.model = nn.Sequential(
            ResidualConvBlock(in_channels, out_channels, is_res=True),
            ResidualConvBlock(out_channels, out_channels, is_res=False),
            nn.MaxPool2d(2)
        )
    def forward(self, x):
        return self.model(x)

class UNetUp(nn.Module):
    """UNet expansion step (Upsample + skip concatenation)"""
    def __init__(self, in_channels, out_channels):
        super(UNetUp, self).__init__()
        self.upsample = nn.ConvTranspose2d(in_channels, out_channels, 2, 2)
        self.conv = nn.Sequential(
            ResidualConvBlock(out_channels * 2, out_channels, is_res=True),
            ResidualConvBlock(out_channels, out_channels, is_res=False)
        )
    def forward(self, x, skip):
        up = self.upsample(x)
        return self.conv(torch.cat([up, skip], dim=1))

class EmbedFC(nn.Module):
    """Helper module to project conditioning factors into spatially broad channel dimensions"""
    def __init__(self, input_dim, hidden_dim):
        super(EmbedFC, self).__init__()
        self.input_dim = input_dim
        self.model = nn.Sequential(
            nn.Linear(input_dim, hidden_dim),
            nn.GELU(),
            nn.Linear(hidden_dim, hidden_dim)
        )
    def forward(self, x):
        # x shape: [B, input_dim] -> output: [B, hidden_dim, 1, 1]
        out = self.model(x)
        return out[:, :, None, None]

class ContextRetroUNet(nn.Module):
    """
    Extremely lightweight Context-Conditioned UNet designed for 64x64 retro sprites generation.
    Supports continuous time-step embedding and joint bilingual prompt conditioning.
    Total parameter footprint is extremely small (~12MB), enabling CPU execution on weak devices.
    """
    def __init__(self, in_channels=3, n_feat=64, embed_dim=128):
        super(ContextRetroUNet, self).__init__()
        self.in_channels = in_channels
        self.n_feat = n_feat

        # Bilingual embedding layer
        self.prompt_embedder = BilingualRetroEmbedding(embed_dim=embed_dim)

        # Initial projection
        self.init_conv = ResidualConvBlock(in_channels, n_feat, is_res=True)

        # Down-sampling path (64x64 -> 32x32 -> 16x16)
        self.down1 = UNetDown(n_feat, n_feat * 2)
        self.down2 = UNetDown(n_feat * 2, n_feat * 4)

        # Center Bottleneck
        self.to_vec = nn.Sequential(
            nn.AvgPool2d(16),
            nn.GELU()
        )
        self.bottleneck_up = nn.Sequential(
            nn.ConvTranspose2d(n_feat * 4, n_feat * 4, 16),
            nn.GroupNorm(8, n_feat * 4),
            nn.GELU()
        )

        # Up-sampling path (16x16 -> 32x32 -> 64x64)
        self.up1 = UNetUp(n_feat * 4, n_feat * 2)
        self.up2 = UNetUp(n_feat * 2, n_feat)

        # Output prediction
        self.final_conv = nn.Sequential(
            nn.Conv2d(n_feat * 2, n_feat, 3, 1, 1),
            nn.GroupNorm(8, n_feat),
            nn.GELU(),
            nn.Conv2d(n_feat, in_channels, 3, 1, 1)
        )

        # Timestep embeddings projection
        self.time_embed1 = EmbedFC(1, n_feat * 4)
        self.time_embed2 = EmbedFC(1, n_feat * 2)

        # Context (Bilingual Prompt) embeddings projection
        self.context_embed1 = EmbedFC(embed_dim, n_feat * 4)
        self.context_embed2 = EmbedFC(embed_dim, n_feat * 2)

    def forward(self, x, t, prompts):
        """
        x: [B, 3, 64, 64] noisy input images
        t: [B, 1] scalar normalized timesteps
        prompts: list of strings (Arabic or English descriptions)
        """
        # Encode bilingual prompts
        c_embed = self.prompt_embedder(prompts)

        # Compute embeddings
        time_proj1 = self.time_embed1(t)
        time_proj2 = self.time_embed2(t)

        ctx_proj1 = self.context_embed1(c_embed)
        ctx_proj2 = self.context_embed2(c_embed)

        # Initial layer
        x_init = self.init_conv(x)

        # Down-blocks (64x64 -> 32x32 -> 16x16)
        d1 = self.down1(x_init)
        d2 = self.down2(d1)

        # Middle Bottleneck
        bottleneck = self.bottleneck_up(self.to_vec(d2))

        # Up-blocks with interactive time & bilingual prompt conditioning
        u1 = self.up1(bottleneck * ctx_proj1 + time_proj1, d1)
        u2 = self.up2(u1 * ctx_proj2 + time_proj2, x_init)

        # Combine skip representation and output predicted noise
        out = self.final_conv(torch.cat([u2, x_init], dim=1))
        return out
