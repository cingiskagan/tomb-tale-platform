import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { PlayerService } from '../../core/api/player.service';
import { Player } from '../../core/api/player.models';
import { MessageService } from 'primeng/api';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-my-profile',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    CardModule,
    InputTextModule,
    ButtonModule,
    ToastModule
  ],
  providers: [MessageService],
  templateUrl: './my-profile.component.html',
  styleUrl: './my-profile.component.css'
})
export class MyProfileComponent implements OnInit {
  player: Player | null = null;
  profileForm: FormGroup;
  isSaving = false;

  private readonly playerService = inject(PlayerService);
  private readonly messageService = inject(MessageService);
  private readonly fb = inject(FormBuilder);

  constructor() {
    this.profileForm = this.fb.group({
      displayName: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(30)]]
    });
  }

  ngOnInit() {
    this.loadProfile();
  }

  loadProfile() {
    this.playerService.getMyProfile().subscribe({
      next: (data) => {
        this.player = data;
        this.profileForm.patchValue({
          displayName: data.displayName
        });
      },
      error: (err) => {
        console.error('Failed to load profile:', err);
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load profile' });
      }
    });
  }

  onSubmit() {
    if (this.profileForm.invalid) {
      return;
    }

    this.isSaving = true;
    const request = {
      displayName: this.profileForm.value.displayName,
      profileIcon: this.player?.profileIcon
    };

    this.playerService.updateMyProfile(request).subscribe({
      next: (updatedPlayer) => {
        this.player = updatedPlayer;
        this.profileForm.markAsPristine();
        this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Profile updated successfully' });
        this.isSaving = false;
      },
      error: (err: HttpErrorResponse) => {
        console.error('Failed to update profile:', err);
        this.isSaving = false;
        if (err.status === 409) {
          this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Display name is already taken' });
        } else {
          this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to update profile' });
        }
      }
    });
  }
}
